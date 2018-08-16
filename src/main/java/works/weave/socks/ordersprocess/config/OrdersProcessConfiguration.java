package works.weave.socks.ordersprocess.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrdersProcessConfiguration {
    @Bean
    @ConditionalOnMissingBean(OrdersProcessConfigurationProperties.class)
    public OrdersProcessConfigurationProperties frameworkMesosConfigProperties() {
        return new OrdersProcessConfigurationProperties();
    }
}
