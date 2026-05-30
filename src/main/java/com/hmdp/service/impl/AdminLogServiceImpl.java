package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.AdminLog;
import com.hmdp.mapper.AdminLogMapper;
import com.hmdp.service.IAdminLogService;
import org.springframework.stereotype.Service;

@Service
public class AdminLogServiceImpl extends ServiceImpl<AdminLogMapper, AdminLog> implements IAdminLogService {
}
