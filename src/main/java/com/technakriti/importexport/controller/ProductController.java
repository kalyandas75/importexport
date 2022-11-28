package com.technakriti.importexport.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.boot.json.JsonParser;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES;


@RequestMapping("/api/products")
@RestController
public class ProductController {

    private static final Pattern p = Pattern.compile("^(window\\.runParams\\s*=\\s*\\{)([\\s\\S]*?)(};)$?");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductController() {
        objectMapper.configure(ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(ALLOW_SINGLE_QUOTES, true);
    }
    @PostMapping("/scrape")
    public Map getProductDetails(@RequestBody String url) throws IOException {
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
                result.put("title", ((Map) data.get("titleModule")).get("subject"));
                var descriptionModule = (Map) data.get("descriptionModule");
                String descriptionUrl = (String) descriptionModule.get("descriptionUrl");
                Document docDescription = Jsoup.connect(descriptionUrl).get();
                //   System.out.println(docDescription.getElementsByAttribute("src"));
                var descTexts = docDescription.body().getElementsByClass("detailmodule_text");
                result.put("description", descTexts.stream().map(d -> d.getElementsByTag("p").text()).collect(Collectors.joining("<br>")));
                result.put("categoryId", ((Map)data.get("actionModule")).get("categoryId"));
                result.put("productId", ((Map)data.get("actionModule")).get("productId"));

                var quantity = new HashMap<>();
                quantity.put("totalAvailable", ((Map)data.get("quantityModule")).get("totalAvailQuantity"));
                quantity.put("minimumBulkOrder", ((Map)data.get("quantityModule")).get("skuBulkOrder"));
                quantity.put("bulkOrderDiscount", ((Map)data.get("quantityModule")).get("skuBulkDiscount"));
                quantity.put("bulkOrderUnit", ((Map)data.get("quantityModule")).get("multiUnitName"));
                quantity.put("tradeCount", ((Map) data.get("titleModule")).get("tradeCount"));
                result.put("quantity", quantity);

                var store = new HashMap<>();
                store.put("name", ((Map)data.get("storeModule")).get("storeName"));
                store.put("companyId", ((Map)data.get("storeModule")).get("companyId"));
                store.put("storeNumber", ((Map)data.get("storeModule")).get("storeNum"));
                store.put("followersCount", ((Map)data.get("storeModule")).get("followingNumber"));
                store.put("positiveCount", ((Map)data.get("storeModule")).get("positiveNum"));
                store.put("positiveRating", ((Map)data.get("storeModule")).get("positiveRate"));
                result.put("store", store);

                var rating = new HashMap<>();
                rating.put("max", 5);
                var titleModule = ((Map) data.get("titleModule"));
                rating.put("average", ((Map)titleModule.get("feedbackRating")).get("averageStar"));
                rating.put("totalCount", ((Map)titleModule.get("feedbackRating")).get("totalValidNum"));
                rating.put("fiveStarCount", ((Map)titleModule.get("feedbackRating")).get("fiveStarNum"));
                rating.put("fourStarCount", ((Map)titleModule.get("feedbackRating")).get("fourStarNum"));
                rating.put("threeStarCount", ((Map)titleModule.get("feedbackRating")).get("threeStarNum"));
                rating.put("twoStarCount", ((Map)titleModule.get("feedbackRating")).get("twoStarNum"));
                rating.put("oneStarCount", ((Map)titleModule.get("feedbackRating")).get("oneStarNum"));
                result.put("rating", rating);

                result.put("images", ((Map)data.get("imageModule")).get("imagePathList"));

                result.put("specs", ((Map)data.get("specsModule")).get("props"));
                result.put("currency", ((Map)data.get("webEnv")).get("currency"));

                var priceModule = ((Map)data.get("priceModule"));
                var listingPrice = new HashMap<>();
                listingPrice.put("min", ((Map)priceModule.get("minAmount")).get("value"));
                listingPrice.put("max", ((Map)priceModule.get("maxAmount")).get("value"));
                result.put("listingPrice", listingPrice);

                var sellingPrice = new HashMap<>();
                sellingPrice.put("min", ((Map)priceModule.get("minActivityAmount")).get("value"));
                sellingPrice.put("max", ((Map)priceModule.get("maxActivityAmount")).get("value"));
                result.put("sellingPrice", sellingPrice);
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        new ProductController().getProductDetails("https://www.aliexpress.us/item/3256803609508209.html");
/*        Pattern p = Pattern.compile("^(window\\.runParams\\s*=\\s*\\{)([\\s\\S]*?)(};)$?");
        String s = FileUtils.readFileToString(new File("D:\\works\\companies\\abdarahman\\importexport\\src\\main\\resources\\dummy.txt"), Charset.forName("UTF-8"));
       // System.out.println(s);
        Matcher m = p.matcher(s);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(ALLOW_SINGLE_QUOTES, true);
        if (m.find()) {
            System.out.println("----------------------");
            Map res = objectMapper.readValue("{" + m.group(2) + "}", Map.class);
            System.out.println(res);
        }*/
    }

}
