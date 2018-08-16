package works.weave.socks.ordersprocess.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import works.weave.socks.ordersprocess.entities.CustomerOrder;

import java.util.List;

@RepositoryRestResource(path = "orders", itemResourceRel = "order")
public interface CustomerOrderProcessRepository extends MongoRepository<CustomerOrder, String> {
    @RestResource(path = "customerId")
    List<CustomerOrder> findByCustomerId(@Param("custId") String id);
}

