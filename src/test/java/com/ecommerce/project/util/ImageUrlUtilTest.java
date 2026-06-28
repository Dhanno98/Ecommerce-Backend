package com.ecommerce.project.util;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ImageUrlUtilTest {

    private final ImageUrlUtil imageUrlUtil = new ImageUrlUtil();

    /// constructImageUrl()
    @Test
    void constructImageUrlShouldSuccessfullyConstructImageUrlWhenBaseUrlEndsWithForwardSlash() {
        ReflectionTestUtils.setField(imageUrlUtil, "imageBaseUrl", "http://localhost:8080/images/");
        String result = imageUrlUtil.constructImageUrl("phone.jpg");

        assertNotNull(result);
        assertEquals("http://localhost:8080/images/phone.jpg", result);
    }

    @Test
    void constructImageUrlShouldSuccessfullyConstructImageUrlWhenBaseUrlDoesNotEndWithForwardSlash() {
        ReflectionTestUtils.setField(imageUrlUtil, "imageBaseUrl", "http://localhost:8080/images");
        String result = imageUrlUtil.constructImageUrl("phone.jpg");

        assertNotNull(result);
        assertEquals("http://localhost:8080/images/phone.jpg", result);
    }
}