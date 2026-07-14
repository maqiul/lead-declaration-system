package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.SysDict;
import com.declaration.entity.SysDictItem;

import java.util.List;

/**
 * 系统字典服务接口
 *
 * @author Administrator
 * @since 2026-07-07
 */
public interface SysDictService extends IService<SysDict> {

    /**
     * 获取所有字典类型列表
     */
    List<SysDict> listAll();

    /**
     * 创建字典类型
     */
    Long createDict(SysDict dict);

    /**
     * 更新字典类型
     */
    void updateDict(Long id, SysDict dict);

    /**
     * 删除字典类型（含级联删除项）
     */
    void deleteDict(Long id);

    /**
     * 获取指定字典编码的字典项列表
     */
    List<SysDictItem> listItemsByDictCode(String dictCode);

    /**
     * 获取启用状态的字典项（公开接口，用于下拉选择）
     */
    List<SysDictItem> listEnabledItems(String dictCode);

    /**
     * 创建字典项
     */
    Long createItem(SysDictItem item);

    /**
     * 更新字典项
     */
    void updateItem(Long id, SysDictItem item);

    /**
     * 删除字典项
     */
    void deleteItem(Long id);
}
