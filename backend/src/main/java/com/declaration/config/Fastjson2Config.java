package com.declaration.config;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Fastjson2配置类
 * 配置JSON序列化和反序列化的行为
 */
@Configuration
public class Fastjson2Config implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();

        // 全局配置序列化特性
        JSONWriter.Feature[] writerFeatures = {
                JSONWriter.Feature.WriteMapNullValue,        // 输出Map的null值
                JSONWriter.Feature.WriteNullListAsEmpty,     // 将null的List输出为[]
                JSONWriter.Feature.WriteNullStringAsEmpty,   // 将null的String输出为""
                JSONWriter.Feature.WriteNullNumberAsZero,    // 将null的Number输出为0
                JSONWriter.Feature.WriteLongAsString         // 将Long转为String,避免前端精度丢失
        };

        // 全局配置反序列化特性
        JSONReader.Feature[] readerFeatures = {
                JSONReader.Feature.FieldBased,               // 基于字段反序列化
                JSONReader.Feature.SupportArrayToBean       // 支持数组到Bean的转换
        };

        config.setWriterFeatures(writerFeatures);
        config.setReaderFeatures(readerFeatures);

        // 添加LocalDateTime格式化
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");

        converter.setFastJsonConfig(config);
        converters.add(0, converter);
    }
}