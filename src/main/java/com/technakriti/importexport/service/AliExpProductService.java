package com.technakriti.importexport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES;

@Service
public class AliExpProductService {
    private static final Pattern p = Pattern.compile("^(window\\.runParams\\s*=\\s*\\{)([\\s\\S]*?)(};)$?");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AliExpProductService() {
        objectMapper.configure(ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(ALLOW_SINGLE_QUOTES, true);
    }

    public Map<String, Object> getProductDetails(String url) throws IOException {
        Map<String, Object> result = new HashMap();
        if(url.contains("aliexpress")) {
            Map runParams = null;
            Document doc = Jsoup.connect(url).get();
            Elements scripts = doc.body().getElementsByTag("script");
            for (Element script : scripts) {
                if (script.html().contains("window.runParams")) {
                    String d = script.html();
                    Matcher m = p.matcher(d);
                    if (m.find()) {
                        runParams = objectMapper.readValue("{" + m.group(2) + "}", Map.class);
                    }
                    break;
                }
            }
            if (!CollectionUtils.isEmpty(runParams)) {
                var data = (Map) runParams.get("data");
                result.put("title", getValue(data, "titleModule", "subject"));
                var descriptionModule = (Map) data.get("descriptionModule");
                String descriptionUrl = (String) descriptionModule.get("descriptionUrl");
                Document docDescription = Jsoup.connect(descriptionUrl).get();
                //   System.out.println(docDescription.getElementsByAttribute("src"));
                var descTexts = docDescription.body().getElementsByClass("detailmodule_text");
                result.put("description", descTexts.stream().map(d -> d.getElementsByTag("p").text()).collect(Collectors.joining("<br>")));
                result.put("categoryId", getValue(data,"actionModule", "categoryId"));
                result.put("productId", getValue(data,"actionModule", "productId"));

                var quantity = new HashMap<>();
                quantity.put("totalAvailable", getValue(data,"quantityModule", "totalAvailQuantity"));
                quantity.put("minimumBulkOrder", getValue(data,"quantityModule", "skuBulkOrder"));
                quantity.put("bulkOrderDiscount",  getValue(data,"quantityModule", "skuBulkDiscount"));
                quantity.put("bulkOrderUnit", getValue(data,"quantityModule", "multiUnitName"));
                quantity.put("tradeCount", getValue(data,"titleModule", "tradeCount"));
                result.put("quantity", quantity);

                var store = new HashMap<>();
                store.put("name", getValue(data,"storeModule", "storeName"));
                store.put("companyId", getValue(data,"storeModule", "companyId"));
                store.put("storeNumber", getValue(data,"storeModule", "storeNum"));
                store.put("followersCount", getValue(data,"storeModule", "followingNumber"));
                store.put("positiveCount", getValue(data,"storeModule", "positiveNum"));
                store.put("positiveRating", getValue(data,"storeModule", "positiveRate"));
                result.put("store", store);

                var rating = new HashMap<>();
                rating.put("max", 5);
                var titleModule = ((Map) data.get("titleModule"));
                rating.put("average", getValue(titleModule,"feedbackRating", "averageStar"));
                rating.put("totalCount", getValue(titleModule,"feedbackRating", "totalValidNum"));
                rating.put("fiveStarCount", getValue(titleModule,"feedbackRating", "fiveStarNum"));
                rating.put("fourStarCount", getValue(titleModule,"feedbackRating", "fourStarNum"));
                rating.put("threeStarCount",  getValue(titleModule,"feedbackRating", "threeStarNum"));
                rating.put("twoStarCount", getValue(titleModule,"feedbackRating", "twoStarNum"));
                rating.put("oneStarCount", getValue(titleModule,"feedbackRating", "oneStarNum"));
                result.put("rating", rating);

                result.put("images", getValue(data,"imageModule", "imagePathList"));

                result.put("specs",  getValue(data,"specsModule", "props"));
                result.put("currency", getValue(data,"webEnv", "currency"));

                var priceModule = ((Map)data.get("priceModule"));
                var listingPrice = new HashMap<>();
                listingPrice.put("min", getValue(priceModule,"minAmount", "value"));
                listingPrice.put("max", getValue(priceModule,"maxAmount", "value"));
                result.put("listingPrice", listingPrice);

                var sellingPrice = new HashMap<>();
                var minActivityAmount = (Map)priceModule.get("minActivityAmount");
                if(minActivityAmount != null) {
                    sellingPrice.put("min", ((Map) priceModule.get("minActivityAmount")).get("value"));
                } else {
                    sellingPrice.put("min", null);
                }
                sellingPrice.put("min", getValue(priceModule,"minActivityAmount", "value"));
                sellingPrice.put("max", getValue(priceModule,"maxActivityAmount", "value"));
                result.put("sellingPrice", sellingPrice);
                result.put("variants", getVariants(((Map)data.get("skuModule"))));
            }
        }
        return result;
    }

    private Map<String, Object> getVariants(Map skuModule) {
        Map<String, Object> variants = new HashMap<>();
        var priceList = (List<Map<String, Object>>)(skuModule.get("skuPriceList"));
        if(CollectionUtils.isEmpty(priceList)) {
            priceList = new ArrayList();
        }
        var optionList = (List<Map<String, Object>>)(skuModule.get("productSKUPropertyList"));
        if(CollectionUtils.isEmpty(optionList)) {
            optionList = new ArrayList();
        }
        variants.put("prices", priceList.stream().map(p -> {
            Map<String, Object> v = new HashMap<>();
            v.put("skuId", p.get("skuId"));
            v.put("optionValueIds", p.get("optionValueIds"));
            v.put("skuPropIds", p.get("skuPropIds"));
            Map<String, Object> skuVal = (Map<String, Object>) p.get("skuVal");
            v.put("availableQuantity", skuVal.get("availQuantity"));
            v.put("originalPrice", getValue(skuVal,"skuAmount", "value"));
            v.put("salePrice", getValue(skuVal,"skuActivityAmount", "value"));
            return v;
        }).collect(Collectors.toList()));
        variants.put("options", optionList.stream().map(o -> {
            Map<String, Object> v = new HashMap<>();
            v.put("skuPropertyId", o.get("skuPropertyId"));
            v.put("skuPropertyName", o.get("skuPropertyName"));
            List<Map<String, Object>> vals = (List<Map<String, Object>>) o.get("skuPropertyValues");
            if(!CollectionUtils.isEmpty(vals)) {
                v.put("skuPropertyValues", vals.stream().map(vs -> {
                    var m = new HashMap<String, Object>();
                    m.put("propertyValueId", vs.get("propertyValueId"));
                    m.put("propertyValueName", vs.get("propertyValueName"));
                    m.put("propertyValueDisplayName", vs.get("propertyValueDisplayName"));
                    m.put("skuPropertyImagePath", vs.get("skuPropertyImagePath"));
                    return m;
                }).collect(Collectors.toList()));
            }
            return v;
        }).collect(Collectors.toList()));
        return variants;
    }

    private Object getValue(Map module, String subModuleName, String subModuleValueKey) {
        if(module == null || module.isEmpty()) {
            return null;
        }
        if(module.get(subModuleName) == null) {
            return null;
        }
        Map<String, Object> subModule = (Map<String, Object>) module.get(subModuleName);
        return subModule.get(subModuleValueKey);
    }
}
