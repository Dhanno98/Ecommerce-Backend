package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.CreateProductRequest;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import com.ecommerce.project.util.ImageUrlUtil;
import com.ecommerce.project.util.PaginationValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;
    
    private final ModelMapper modelMapper;

    private final FileService fileService;

    private final CartItemRepository cartItemRepository;

    private final PaginationValidator paginationValidator;

    private final static List<String> ALLOWED_SORT_FIELDS = List.of("productId", "productName", "quantity", "price", "specialPrice");

    private final static List<String> ALLOWED_CONTENT_TYPES = List.of("image/png", "image/jpeg", "image/webp");

    private final ImageUrlUtil imageUrlUtil;

    @Value("${project.image}")
    private String path;

    private final AuthUtil authUtil;

    @Override
    public ProductDTO addProduct(Long categoryId, CreateProductRequest productRequest) {
        log.info("Product creation requested. productName={}, categoryId={}, sellerId={}",
                productRequest.getProductName(), categoryId, authUtil.loggedInUserId());

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        if (productRepository.existsByProductNameIgnoreCase(productRequest.getProductName())) {
            log.warn("Product creation failed. Product already exists. productName={}", productRequest.getProductName());
            throw new APIException("Product with name: " + productRequest.getProductName() + " already exists.");
        }

        Product product = modelMapper.map(productRequest, Product.class);
        product.setImage("default.png");
        product.setCategory(category);
        product.setUser(authUtil.loggedInUser());
        BigDecimal specialPrice = calculateSpecialPrice(product.getPrice(), product.getDiscount());
        product.setSpecialPrice(specialPrice);

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully. productId={}, productName={}",
                savedProduct.getProductId(), savedProduct.getProductName());

        return mapToDTO(savedProduct);
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String category) {
        paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Specification<Product> spec = getProductSpecification(keyword, category);

        Page<Product> productPage = productRepository.findAll(spec, pageDetails);

        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTOS = products.stream()
                .map(this::mapToDTO)
                .toList();

        return buildProductResponse(productPage, productDTOS);
    }

    @Override
    public ProductResponse getAllProductsForAdmin(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String category) {
        paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Specification<Product> spec = getProductSpecification(keyword, category);

        Page<Product> productPage = productRepository.findAll(spec, pageDetails);

        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTOS = products.stream()
                .map(this::mapToDTO)
                .toList();

        return buildProductResponse(productPage, productDTOS);
    }

    @Override
    public ProductResponse getAllProductsForSeller(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        User user = authUtil.loggedInUser();
        Page<Product> productPage = productRepository.findByUser(user, pageDetails);

        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTOS = products.stream()
                .map(this::mapToDTO)
                .toList();

        return buildProductResponse(productPage, productDTOS);
    }

    @Override
    public ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepository.findByCategory(category, pageDetails);

        List<Product> products = productPage.getContent();
        List<ProductDTO> productDTOS = products.stream()
                .map(this::mapToDTO)
                .toList();

        return buildProductResponse(productPage, productDTOS);
    }

    @Override
    public ProductResponse searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productPage = productRepository.findByProductNameContainingIgnoreCase(keyword, pageDetails);

        List<Product> products = productPage.getContent();
        List<ProductDTO> productDTOS = products.stream()
                .map(this::mapToDTO)
                .toList();

        return buildProductResponse(productPage, productDTOS);
    }

    @Transactional
    @Override
    public ProductDTO updateProduct(CreateProductRequest productRequest, Long productId) {
        log.info("Product update requested. productId={}", productId);

        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (productRepository.existsByProductNameIgnoreCaseAndProductIdNot(productRequest.getProductName(), productId)) {
            log.warn("Product update failed. Duplicate product name={}", productRequest.getProductName());
            throw new APIException("Product with name: " + productRequest.getProductName() + " already exists.");
        }

        productFromDB.setProductName(productRequest.getProductName());
        productFromDB.setDescription(productRequest.getDescription());
        productFromDB.setQuantity(productRequest.getQuantity());
        productFromDB.setPrice(productRequest.getPrice());
        productFromDB.setDiscount(productRequest.getDiscount());
        BigDecimal specialPrice = calculateSpecialPrice(productRequest.getPrice(), productRequest.getDiscount());
        productFromDB.setSpecialPrice(specialPrice);

        List<CartItem> cartItems = cartItemRepository.findAllByProductId(productFromDB.getProductId());
        for (CartItem cartItem : cartItems) {
            Cart cart = cartItem.getCart();
            BigDecimal oldPrice = cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            cartItem.setDiscount(productFromDB.getDiscount());
            cartItem.setProductPrice(specialPrice);

            BigDecimal newPrice = specialPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            cart.setTotalPrice(cart.getTotalPrice().subtract(oldPrice).add(newPrice));
        }

        log.info("Product updated successfully. productId={}, productName={}",
                productFromDB.getProductId(), productFromDB.getProductName());

        return mapToDTO(productFromDB);
    }

    @Transactional
    @Override
    public ProductDTO updateProductSeller(CreateProductRequest productRequest, Long productId) {
        User seller = authUtil.loggedInUser();

        log.info("Seller product update requested. sellerId={}, productId={}", seller.getUserId(), productId);

        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (!productFromDB.getUser().getUserId().equals(seller.getUserId())) {
            log.warn("Seller attempted to update product not owned by them. sellerId={}, productId={}",
                    seller.getUserId(), productId);
            throw new APIException("Product cannot be updated as it does not belong to this seller!");
        }

        if (productRepository.existsByProductNameIgnoreCaseAndProductIdNot(
                productRequest.getProductName(), productId)) {
            log.warn("Seller product update failed. Duplicate product name={}", productRequest.getProductName());
            throw new APIException("Product with name: " + productRequest.getProductName() + " already exists.");
        }

        productFromDB.setProductName(productRequest.getProductName());
        productFromDB.setDescription(productRequest.getDescription());
        productFromDB.setQuantity(productRequest.getQuantity());
        productFromDB.setPrice(productRequest.getPrice());
        productFromDB.setDiscount(productRequest.getDiscount());
        BigDecimal specialPrice = calculateSpecialPrice(productRequest.getPrice(), productRequest.getDiscount());
        productFromDB.setSpecialPrice(specialPrice);

        List<CartItem> cartItems = cartItemRepository.findAllByProductId(productFromDB.getProductId());
        for (CartItem cartItem : cartItems) {
            Cart cart = cartItem.getCart();
            BigDecimal oldPrice = cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            cartItem.setDiscount(productFromDB.getDiscount());
            cartItem.setProductPrice(specialPrice);

            BigDecimal newPrice = specialPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            cart.setTotalPrice(cart.getTotalPrice().subtract(oldPrice).add(newPrice));
        }

        log.info("Seller product updated successfully. sellerId={}, productId={}", seller.getUserId(), productId);
        return mapToDTO(productFromDB);
    }

    @Transactional
    @Override
    public ProductDTO deleteProduct(Long productId) {
        log.info("Product deletion requested. productId={}", productId);

        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        List<CartItem> cartItems = cartItemRepository.findAllByProductId(productId);
        for (CartItem cartItem : cartItems) {
            Cart cart = cartItem.getCart();
            BigDecimal deduction = cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            cart.setTotalPrice(cart.getTotalPrice().subtract(deduction));
        }
        cartItemRepository.deleteAll(cartItems);
        productRepository.delete(productFromDB);

        log.info("Product deleted successfully. productId={}, productName={}",
                productFromDB.getProductId(), productFromDB.getProductName());

        return mapToDTO(productFromDB);
    }

    @Transactional
    @Override
    public ProductDTO deleteProductSeller(Long productId) {
        User seller = authUtil.loggedInUser();

        log.info("Seller product deletion requested. sellerId={}, productId={}", seller.getUserId(), productId);

        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (!productFromDB.getUser().getUserId().equals(seller.getUserId())) {
            log.warn("Seller attempted to delete product not owned by them. sellerId={}, productId={}",
                    seller.getUserId(), productId);
            throw new APIException("Product cannot be deleted as it does not belong to this seller!");
        }

        List<CartItem> cartItems = cartItemRepository.findAllByProductId(productId);
        for (CartItem cartItem : cartItems) {
            Cart cart = cartItem.getCart();
            BigDecimal deduction = cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            cart.setTotalPrice(cart.getTotalPrice().subtract(deduction));
        }
        cartItemRepository.deleteAll(cartItems);
        productRepository.delete(productFromDB);

        log.info("Seller product deleted successfully. sellerId={}, productId={}", seller.getUserId(), productId);
        return mapToDTO(productFromDB);
    }

    @Transactional
    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        log.info("Product image update requested. productId={}", productId);

        if (image.isEmpty()) {
            log.warn("Image upload failed. Empty image. productId={}", productId);
            throw new APIException("Image cannot be empty");
        }

        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(image.getContentType())) {
            log.warn("Image upload failed. Invalid content type={}. productId={}", contentType, productId);
            throw new APIException("Invalid image type. Valid image types: PNG, JPEG, WEBP");
        }

        if (image.getSize() > 5 * 1024 * 1024) {
            log.warn("Image upload failed. File too large. size={}, productId={}", image.getSize(), productId);
            throw new APIException("Image size exceeds 5MB");
        }

        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        String fileName = fileService.uploadImage(path, image);

        productFromDB.setImage(fileName);

        Product updatedProduct = productRepository.save(productFromDB);

        log.info("Product image updated successfully. productId={}, image={}",
                updatedProduct.getProductId(), updatedProduct.getImage());
        return mapToDTO(updatedProduct);
    }

    @Transactional
    @Override
    public ProductDTO updateProductImageSeller(Long productId, MultipartFile image) throws IOException {
        User seller = authUtil.loggedInUser();

        log.info("Seller image update requested. sellerId={}, productId={}", seller.getUserId(), productId);

        if (image.isEmpty()) {
            log.warn("Seller image upload failed. Empty image. sellerId={}, productId={}", seller.getUserId(), productId);
            throw new APIException("Image cannot be empty");
        }

        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(image.getContentType())) {
            log.warn("Seller image upload failed. Invalid content type={}. productId={}, sellerId={}",
                    contentType, productId, seller.getUserId());
            throw new APIException("Invalid image type. Valid image types: PNG, JPEG, WEBP");
        }

        if (image.getSize() > 5 * 1024 * 1024) {
            log.warn("Image upload failed. File too large. size={}, productId={}, sellerId={}",
                    image.getSize(), productId, seller.getUserId());
            throw new APIException("Image size exceeds 5MB");
        }

        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (!productFromDB.getUser().getUserId().equals(seller.getUserId())) {
            log.warn("Seller attempted to update image of product not owned by them. sellerId={}, productId={}",
                    seller.getUserId(), productId);
            throw new APIException("Product image cannot be updated as it does not belong to this seller!");
        }

        String fileName = fileService.uploadImage(path, image);

        productFromDB.setImage(fileName);

        Product updatedProduct = productRepository.save(productFromDB);

        log.info("Seller product image updated successfully. sellerId={}, productId={}, image={}",
                seller.getUserId(), productId, updatedProduct.getImage());
        return mapToDTO(updatedProduct);
    }

    private static @NonNull Specification<Product> getProductSpecification(String keyword, String category) {
        Specification<Product> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
        }

        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(criteriaBuilder.lower(root.get("category").get("categoryName")), category.toLowerCase()));
        }
        return spec;
    }

    private ProductDTO mapToDTO(Product product) {
        ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
        productDTO.setImage(imageUrlUtil.constructImageUrl(product.getImage()));
        return productDTO;
    }

    private BigDecimal calculateSpecialPrice(BigDecimal price, BigDecimal discount) {
        BigDecimal discountPercentage = discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal discountAmount = price.multiply(discountPercentage);
        return price.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private ProductResponse buildProductResponse(Page<Product> productPage, List<ProductDTO> productDTOS) {
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

}
