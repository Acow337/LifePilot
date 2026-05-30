package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);

    Result adjustSeckillStock(Long voucherId, Integer stock);

    Result queryAdminVouchers(Integer page, Integer size, Integer type, Long shopId, Integer status, String title);

    Result createAdminVoucher(Voucher voucher);

    Result updateAdminVoucher(Long voucherId, Voucher voucher);

    Result updateVoucherStatus(Long voucherId, Integer status);
}
