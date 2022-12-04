package com.technakriti.importexport.controller;

import com.technakriti.importexport.service.AliExpProductService;
import com.technakriti.importexport.service.ShopifyProductService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;


@RequestMapping("/api/products")
@RestController
public class ProductController {

    private final AliExpProductService aliExpProductService;
    private final ShopifyProductService shopifyProductService;

    public ProductController(AliExpProductService aliExpProductService, ShopifyProductService shopifyProductService) {
        this.aliExpProductService = aliExpProductService;
        this.shopifyProductService = shopifyProductService;
    }

    @PostMapping("/scrape")
    public Map getProductDetails(@RequestBody String url) throws IOException {
        if(url.contains("aliexpress")) {
            return aliExpProductService.getProductDetails(url);
        } else {
            return shopifyProductService.getProductDetails(url);
        }
    }
}
