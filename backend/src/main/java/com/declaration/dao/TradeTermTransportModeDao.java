package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 贸易方式与运输方式关联DAO
 */
public interface TradeTermTransportModeDao extends BaseMapper<Object> {

    @Select("SELECT transport_mode_code FROM trade_term_transport_mode WHERE trade_term_code = #{tradeTermCode}")
    List<String> selectTransportModesByTradeTermCode(@Param("tradeTermCode") String tradeTermCode);

    @Insert("INSERT INTO trade_term_transport_mode (trade_term_code, transport_mode_code) VALUES (#{tradeTermCode}, #{transportModeCode})")
    int insert(@Param("tradeTermCode") String tradeTermCode, @Param("transportModeCode") String transportModeCode);

    @Delete("DELETE FROM trade_term_transport_mode WHERE trade_term_code = #{tradeTermCode}")
    int deleteByTradeTermCode(@Param("tradeTermCode") String tradeTermCode);
}
