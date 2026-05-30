package com.hmdp.controller;

import com.hmdp.annotation.AdminActionLog;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/shops")
public class AdminShopController {

    @Resource
    private IShopService shopService;

    @GetMapping
    public Result queryShops(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "typeId", required = false) Long typeId
    ) {
        return shopService.queryAdminShops(page, size, keyword, typeId);
    }

    @AdminActionLog(module = "shop", action = "create", targetType = "shop", targetId = "#result.data")
    @PostMapping
    public Result createShop(@RequestBody Shop shop) {
        return shopService.createAdminShop(shop);
    }

    @AdminActionLog(module = "shop", action = "update", targetType = "shop", targetId = "#shopId")
    @PutMapping("/{id}")
    public Result updateShop(@PathVariable("id") Long shopId,
                             @RequestBody Shop shop) {
        return shopService.updateAdminShop(shopId, shop);
    }
}
