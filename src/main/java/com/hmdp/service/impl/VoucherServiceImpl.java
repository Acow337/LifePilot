package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.SECKILL_BEGIN_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_END_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    private static final int VOUCHER_TYPE_SECKILL = 1;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        save(voucher);
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        refreshSeckillMetaInRedis(voucher.getId(), voucher.getStock(), voucher.getBeginTime(), voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
    }

    @Override
    public Result adjustSeckillStock(Long voucherId, Integer stock) {
        if (stock == null || stock < 0) {
            return Result.fail("库存不能小于0");
        }
        boolean success = seckillVoucherService.updateStock(voucherId, stock);
        if (!success) {
            return Result.fail("秒杀券不存在");
        }
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucherId, stock.toString());
        return Result.ok();
    }

    @Override
    public Result queryAdminVouchers(Integer page, Integer size, Integer type, Long shopId, Integer status, String title) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? SystemConstants.MAX_PAGE_SIZE : Math.min(size, 50);
        Page<Voucher> voucherPage = query()
                .eq(type != null, "type", type)
                .eq(shopId != null, "shop_id", shopId)
                .eq(status != null, "status", status)
                .like(StrUtil.isNotBlank(title), "title", title)
                .orderByDesc("create_time")
                .page(new Page<>(currentPage, pageSize));

        List<Voucher> records = new ArrayList<>(voucherPage.getRecords());
        for (Voucher record : records) {
            if (record.getType() != null && record.getType() == VOUCHER_TYPE_SECKILL) {
                SeckillVoucher seckillVoucher = seckillVoucherService.getById(record.getId());
                if (seckillVoucher != null) {
                    record.setStock(seckillVoucher.getStock());
                    record.setBeginTime(seckillVoucher.getBeginTime());
                    record.setEndTime(seckillVoucher.getEndTime());
                }
            }
        }
        return Result.ok(records, voucherPage.getTotal());
    }

    @Override
    @Transactional
    public Result createAdminVoucher(Voucher voucher) {
        if (voucher == null) {
            return Result.fail("优惠券参数不能为空");
        }
        if (voucher.getStatus() == null) {
            voucher.setStatus(1);
        }
        if (voucher.getType() != null && voucher.getType() == VOUCHER_TYPE_SECKILL) {
            if (voucher.getStock() == null || voucher.getBeginTime() == null || voucher.getEndTime() == null) {
                return Result.fail("秒杀券需要传入库存、生效时间、失效时间");
            }
            addSeckillVoucher(voucher);
            return Result.ok(voucher.getId());
        }
        boolean success = save(voucher);
        return success ? Result.ok(voucher.getId()) : Result.fail("创建优惠券失败");
    }

    @Override
    @Transactional
    public Result updateAdminVoucher(Long voucherId, Voucher voucher) {
        if (voucherId == null || voucher == null) {
            return Result.fail("参数不能为空");
        }
        Voucher existed = getById(voucherId);
        if (existed == null) {
            return Result.fail("优惠券不存在");
        }

        if (voucher.getType() != null && !voucher.getType().equals(existed.getType())) {
            return Result.fail("暂不支持修改优惠券类型");
        }

        voucher.setId(voucherId);
        boolean updated = updateById(voucher);
        if (!updated) {
            return Result.fail("更新优惠券失败");
        }

        if (existed.getType() != null && existed.getType() == VOUCHER_TYPE_SECKILL) {
            SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
            if (seckillVoucher == null) {
                seckillVoucher = new SeckillVoucher();
                seckillVoucher.setVoucherId(voucherId);
            }
            if (voucher.getStock() != null) {
                seckillVoucher.setStock(voucher.getStock());
            }
            if (voucher.getBeginTime() != null) {
                seckillVoucher.setBeginTime(voucher.getBeginTime());
            }
            if (voucher.getEndTime() != null) {
                seckillVoucher.setEndTime(voucher.getEndTime());
            }
            seckillVoucherService.saveOrUpdate(seckillVoucher);
            refreshSeckillMetaInRedis(voucherId, seckillVoucher.getStock(), seckillVoucher.getBeginTime(), seckillVoucher.getEndTime());
        }
        return Result.ok();
    }

    @Override
    public Result updateVoucherStatus(Long voucherId, Integer status) {
        if (voucherId == null || status == null) {
            return Result.fail("参数不能为空");
        }
        if (status != 0 && status != 1) {
            return Result.fail("状态仅支持 0(下架) 或 1(生效)");
        }
        Voucher existed = getById(voucherId);
        if (existed == null) {
            return Result.fail("优惠券不存在");
        }
        boolean success = update().set("status", status).eq("id", voucherId).update();
        return success ? Result.ok() : Result.fail("更新优惠券状态失败");
    }

    private void refreshSeckillMetaInRedis(Long voucherId, Integer stock, java.time.LocalDateTime beginTime, java.time.LocalDateTime endTime) {
        if (voucherId == null || stock == null || beginTime == null || endTime == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucherId, stock.toString());
        stringRedisTemplate.opsForValue().set(SECKILL_BEGIN_KEY + voucherId, String.valueOf(beginTime.atZone(ZoneId.systemDefault()).toEpochSecond()));
        stringRedisTemplate.opsForValue().set(SECKILL_END_KEY + voucherId, String.valueOf(endTime.atZone(ZoneId.systemDefault()).toEpochSecond()));

        long keepSeconds = Math.max(3600L,
                endTime.atZone(ZoneId.systemDefault()).toEpochSecond() - java.time.Instant.now().getEpochSecond() + TimeUnit.DAYS.toSeconds(1));
        stringRedisTemplate.expire(SECKILL_STOCK_KEY + voucherId, keepSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.expire(SECKILL_BEGIN_KEY + voucherId, keepSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.expire(SECKILL_END_KEY + voucherId, keepSeconds, TimeUnit.SECONDS);
    }
}
