package com.xd.pre.modules.data.datascope;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.handlers.AbstractSqlParserHandler;
import com.xd.pre.common.exception.PreBaseException;
import com.xd.pre.security.PreSecurityUser;
import com.xd.pre.security.util.SecurityUtil;
import com.xd.pre.modules.data.enums.DataScopeTypeEnum;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Classname DataScopeInterceptor
 * @Description Mybatis 拦截器 主要用于数据权限拦截
 * @Author Created by LiHaodong (alias:小东啊) lihaodongmail@163.com
 * @Date 2019-06-08 10:29
 * @Version 1.0
 */
@Order(90)
@Slf4j
@AllArgsConstructor
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
@Component
public class DataScopeInterceptor extends AbstractSqlParserHandler implements Interceptor {

    private DataSource dataSource;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        this.sqlParser(metaObject);
        // 先判断是否SELECT操作 不是则跳过检查
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        if (!SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType())) {
            return invocation.proceed();
        }
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        // 执行的SQL语句
        String originalSql = boundSql.getSql();
        // SQL语句的参数
        Object parameterObject = boundSql.getParameterObject();

        //查找参数中包含DataScope类型的参数
        DataScope dataScope = findDataScopeObject(parameterObject);
        if (ObjectUtil.isNull(dataScope)) {
            return invocation.proceed();
        }
        String scopeName = dataScope.getScopeName();
        List<Integer> deptIds = dataScope.getDeptIds();
        // 优先获取赋值数据
        if (CollUtil.isEmpty(deptIds)) {
            PreSecurityUser user = SecurityUtil.getUser();
            if (user == null) {
                throw new PreBaseException("auto datascope, set up security details true");
            }

            // 通过角色Id，查询数据权限范围(组织机构)
            Entity query = Db.use(dataSource)
                    .query("select  u.user_id,\n" +
                            "        r.role_id,\n" +
                            "        r.ds_type,\n" +
                            "        case ds_type \n" +
                            "         when 1 then r.ds_scope\n" +
                            "         when 2 then u.dept_id\n" +
                            "         when 3 then dept_childlist(u.dept_id)\n" +
                            "         when 4 then r.ds_scope\n" +
                            "        else '0' end ds_scope\n" +
                            "  from sys_user u, sys_user_role ur, sys_role r\n" +
                            " where u.user_id = ur.user_id and \n" +
                            "       ur.role_id = r.role_id\n" +
                            "       and u.user_id = " + user.getUserId())
                    .stream().max(Comparator.comparingInt(o -> o.getInt("ds_type"))).get();
            // 数据库权限范围字段
            Integer dsType = query.getInt("ds_type");
            // 获取自定义 本级及其下级 查询本级
            if (DataScopeTypeEnum.ALL.getType() != dsType) {
                String dsScope = query.getStr("ds_scope");
                deptIds.addAll(Arrays.stream(dsScope.split(","))
                        .map(Integer::parseInt).collect(Collectors.toList()));
                String join = CollectionUtil.join(deptIds, ",");
                originalSql = "select * from (" + originalSql + ") temp_data_scope where temp_data_scope." + scopeName + " in (" + join + ")";
                metaObject.setValue("delegate.boundSql.sql", originalSql);
            }
            return invocation.proceed();
        }
        return invocation.proceed();

    }

    /**
     * 生成拦截对象的代理
     *
     * @param target 目标对象
     * @return 代理对象
     */
    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    /**
     * mybatis配置的属性
     *
     * @param properties mybatis配置的属性
     */
    @Override
    public void setProperties(Properties properties) {

    }

    /**
     * 查找参数是否包括DataScope对象
     *
     * @param parameterObj 参数列表
     * @return DataScope
     */
    private DataScope findDataScopeObject(Object parameterObj) {
        if (parameterObj instanceof DataScope) {
            return (DataScope) parameterObj;
        } else if (parameterObj instanceof Map) {
            for (Object val : ((Map<?, ?>) parameterObj).values()) {
                if (val instanceof DataScope) {
                    return (DataScope) val;
                }
            }
        }
        return null;
    }
}
