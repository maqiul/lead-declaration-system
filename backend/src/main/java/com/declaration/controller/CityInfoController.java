package com.declaration.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.CityInfo;
import com.declaration.service.ICityInfoService;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 城市信息控制器
 */
@RestController
@RequestMapping("/v1/cities")
public class CityInfoController {

    @Autowired
    private ICityInfoService cityInfoService;

    /**
     * 获取城市信息分页列表
     */
    @GetMapping
    @RequiresPermissions("system:city-info:list")
    public Result<Page<CityInfo>> getCities(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String cityName,
            @RequestParam(required = false) String provinceName,
            @RequestParam(required = false) String countryName,
            @RequestParam(required = false) Integer status
    ) {
        Page<CityInfo> page = cityInfoService.getPageList(pageNum, pageSize, cityName, provinceName, countryName, status);
        return Result.success(page);
    }
    @GetMapping("/{country}/cities")
    @RequiresPermissions("system:city-info:list")
    public Result<List<CityInfo>> getCityCities(@PathVariable String country) throws UnsupportedEncodingException {
        country = URLDecoder.decode(country,"UTF-8");
        System.out.println("国家:"+country);
        if(country == null) 
            return Result.fail("国家不能为空");

        return Result.success(cityInfoService.getCitiesByCountry(country));
    }

    /**
     * 根据ID获取城市信息
     */
    @GetMapping("/{id}")
    @RequiresPermissions("system:city-info:query")
    public Result<CityInfo> getCityById(@PathVariable Long id) {
        CityInfo cityInfo = cityInfoService.getInfoById(id);
        if (cityInfo != null) {
            return Result.success(cityInfo);
        }
        return Result.fail("城市信息不存在");
    }

    /**
     * 新增城市信息
     */
    @PostMapping
    @RequiresPermissions("system:city-info:add")
    public Result<Void> addCity(@RequestBody CityInfo cityInfo) {
        cityInfoService.addCityInfo(cityInfo);
        return Result.success();
    }

    /**
     * 修改城市信息
     */
    @PutMapping("/{id}")
    @RequiresPermissions("system:city-info:update")
    public Result<Void> updateCity(@PathVariable Long id, @RequestBody CityInfo cityInfo) {
        cityInfo.setId(id);
        cityInfoService.updateCityInfo(cityInfo);
        return Result.success();
    }

    /**
     * 删除城市信息
     */
    @DeleteMapping("/{id}")
    @RequiresPermissions("system:city-info:delete")
    public Result<Void> deleteCity(@PathVariable Long id) {
        cityInfoService.deleteCityInfo(id);
        return Result.success();
    }

    /**
     * 更新城市状态
     */
    @PutMapping("/{id}/status")
    @RequiresPermissions("system:city-info:update")
    public Result<Void> updateCityStatus(@PathVariable Long id, @RequestParam Integer status) {
        cityInfoService.updateStatus(id, status);
        return Result.success();
    }

    // /**
    //  * 获取城市省份列表
    //  */
    // @GetMapping("/provinces")
    // @RequiresPermissions("system:city-info:list")
    // public Result<?> getCityProvinces() {
    //     return Result.success(cityInfoService.getProvinceList());
    // }

    // /**
    //  * 搜索城市
    //  */
    // @GetMapping("/search")
    // @RequiresPermissions("system:city-info:query")
    // public Result<?> searchCities(@RequestParam String keyword) {
    //     return Result.success(cityInfoService.searchCities(keyword));
    // }
}