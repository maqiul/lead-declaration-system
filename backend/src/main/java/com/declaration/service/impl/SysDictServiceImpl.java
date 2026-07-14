package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.SysDictDao;
import com.declaration.dao.SysDictItemDao;
import com.declaration.entity.SysDict;
import com.declaration.entity.SysDictItem;
import com.declaration.service.SysDictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统字典服务实现
 *
 * @author Administrator
 * @since 2026-07-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl extends ServiceImpl<SysDictDao, SysDict>
        implements SysDictService {

    private final SysDictItemDao dictItemDao;

    @Override
    public List<SysDict> listAll() {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysDict::getId);
        return this.list(wrapper);
    }

    @Override
    public Long createDict(SysDict dict) {
        // 检查 dictCode 唯一性
        boolean exists = this.lambdaQuery()
                .eq(SysDict::getDictCode, dict.getDictCode())
                .exists();
        if (exists) {
            throw new RuntimeException("字典编码 '" + dict.getDictCode() + "' 已存在");
        }
        if (dict.getStatus() == null) {
            dict.setStatus(1);
        }
        this.save(dict);
        log.info("创建字典类型: code={}, name={}", dict.getDictCode(), dict.getDictName());
        return dict.getId();
    }

    @Override
    public void updateDict(Long id, SysDict dict) {
        SysDict existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("字典类型不存在: id=" + id);
        }
        // 不允许修改 dictCode
        dict.setDictCode(null);
        dict.setId(id);
        this.updateById(dict);
        log.info("更新字典类型: id={}, code={}", id, existing.getDictCode());
    }

    @Override
    @Transactional
    public void deleteDict(Long id) {
        SysDict existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("字典类型不存在: id=" + id);
        }
        // 级联删除字典项
        LambdaQueryWrapper<SysDictItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(SysDictItem::getDictCode, existing.getDictCode());
        dictItemDao.delete(itemWrapper);
        // 删除字典类型
        this.removeById(id);
        log.info("删除字典类型: id={}, code={}, 项数={}",
                id, existing.getDictCode(),
                dictItemDao.selectCount(new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictCode, existing.getDictCode())));
    }

    @Override
    public List<SysDictItem> listItemsByDictCode(String dictCode) {
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictItem::getDictCode, dictCode)
               .orderByAsc(SysDictItem::getSortOrder)
               .orderByAsc(SysDictItem::getId);
        return dictItemDao.selectList(wrapper);
    }

    @Override
    public List<SysDictItem> listEnabledItems(String dictCode) {
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictItem::getDictCode, dictCode)
               .eq(SysDictItem::getStatus, 1)
               .orderByAsc(SysDictItem::getSortOrder)
               .orderByAsc(SysDictItem::getId);
        return dictItemDao.selectList(wrapper);
    }

    @Override
    public Long createItem(SysDictItem item) {
        // 检查字典类型是否存在
        boolean dictExists = this.lambdaQuery()
                .eq(SysDict::getDictCode, item.getDictCode())
                .exists();
        if (!dictExists) {
            throw new RuntimeException("字典类型不存在: code=" + item.getDictCode());
        }
        if (item.getStatus() == null) {
            item.setStatus(1);
        }
        if (item.getSortOrder() == null) {
            item.setSortOrder(0);
        }
        dictItemDao.insert(item);
        log.info("创建字典项: dictCode={}, value={}, label={}",
                item.getDictCode(), item.getItemValue(), item.getItemLabel());
        return item.getId();
    }

    @Override
    public void updateItem(Long id, SysDictItem item) {
        SysDictItem existing = dictItemDao.selectById(id);
        if (existing == null) {
            throw new RuntimeException("字典项不存在: id=" + id);
        }
        // 不允许修改 dictCode（防止跨字典移动）
        item.setDictCode(null);
        item.setId(id);
        dictItemDao.updateById(item);
        log.info("更新字典项: id={}", id);
    }

    @Override
    public void deleteItem(Long id) {
        SysDictItem existing = dictItemDao.selectById(id);
        if (existing == null) {
            throw new RuntimeException("字典项不存在: id=" + id);
        }
        dictItemDao.deleteById(id);
        log.info("删除字典项: id={}, value={}", id, existing.getItemValue());
    }
}
