package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerDTO {
    private Long userId;
    private String username;
    private String email;
    private Set<String> roles = new HashSet<>();
    private List<AddressDTO> addresses = new ArrayList<>();
}
