package cn.cathead.ai.infrastructure.persistent.dao;

import cn.cathead.ai.infrastructure.persistent.po.ModelValidationRulePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IValidationRuleDao {

    List<ModelValidationRulePO> selectEnabledRules(@Param("provider") String provider, @Param("type") String type);

    List<ModelValidationRulePO> selectRules(@Param("provider") String provider, @Param("type") String type);

    int insertRule(@Param("r") ModelValidationRulePO rule);

    int updateRule(@Param("r") ModelValidationRulePO rule);

    int deleteRule(@Param("id") Long id);
}


