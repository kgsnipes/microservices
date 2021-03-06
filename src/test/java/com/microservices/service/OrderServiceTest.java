package com.microservices.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microservices.dto.PlaceOrderRequest;
import com.microservices.dto.PlaceOrderResponse;
import com.microservices.exception.InvalidProductException;
import com.microservices.models.Order;
import com.microservices.models.OrderEntry;
import com.microservices.models.OrderStatus;
import com.microservices.models.Product;
import com.microservices.models.User;
import com.microservices.services.DataLoadService;
import com.microservices.services.OrderService;
import com.microservices.services.ProductService;
import com.microservices.services.UserService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderServiceTest {
	
	@Autowired
	ProductService productService;
	
	@Autowired
	DataLoadService dataLoadService;
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	UserService userService;

	
	private static final Logger LOG = LoggerFactory.getLogger(OrderServiceTest.class);
	
	
	
	@Before
	public void setup() throws Exception
	{
		if(productService.getProductByName("Apple")==null)
		{
			
			dataLoadService.loadDataIntoSystem();
		}
		
	}
	
	//@After
	public void tearDown() throws Exception
	{
		productService.removeProducts(productService.getAllProducts());
	}
	
	@Ignore
	@Test
	public void createOrderTest()
	{
		Order order=orderService.createOrder(new Order());
		LOG.info("Order Created with ID : "+order.getId());
		assertEquals(order.getId()!=null && order.getStatus().equals(OrderStatus.NEW),true);
	}
	
	

	@Test
	public void placeOrderTest() throws InvalidProductException, JsonProcessingException
	{
		Order order=orderService.createOrder(new Order());
		User user=userService.getUserByEmailAddress("user1@test.com");
		
		if(user==null)
		{
			userService.saveUser(new User("user1","user1@test.com"));
		}
		
		Product product=productService.getProductByName("Apple");
		
		List<OrderEntry> entries=new ArrayList<>();
		
		LOG.info("Order Created with ID : "+order.getId());
		OrderEntry entry=new OrderEntry();
		
		entry.setProductId(product.getId());
		entry.setQty(10);
		entry.setUnitPrice(product.getPrice());
		entry.setOwner(order);
		entries.add(entry);
		
		order.setOrderEntries(entries);
		order.setCustomer(user);
		orderService.saveOrder(order);
		
		PlaceOrderRequest request=new PlaceOrderRequest();
		request.setOrder(order);
		
		PlaceOrderResponse response=orderService.placeOrder(request);
		
		assertEquals(response!=null,true);
		assertEquals(response.getOrder()!=null,true);
		assertEquals(response.getOrder().getStatus().equals(OrderStatus.PLACED),true);
		
	}
	
	
	@Test
	public void placeOrderTestForOutOfStock() throws InvalidProductException, JsonProcessingException
	{
		Order order=orderService.createOrder(new Order());
		User user=userService.getUserByEmailAddress("user2@test.com");
		
		if(user==null)
		{
			userService.saveUser(new User("user2","user2@test.com"));
		}
		Product product=productService.getProductByName("Apple");
		
		List<OrderEntry> entries=new ArrayList<>();
		
		LOG.info("Order Created with ID : "+order.getId());
		OrderEntry entry=new OrderEntry();
		
		entry.setProductId(product.getId());
		entry.setQty(1000);
		entry.setUnitPrice(product.getPrice());
		entry.setOwner(order);
		entries.add(entry);
		
		order.setOrderEntries(entries);
		order.setCustomer(user);
		orderService.saveOrder(order);
		
		PlaceOrderRequest request=new PlaceOrderRequest();
		request.setOrder(order);
		
		PlaceOrderResponse response=orderService.placeOrder(request);
		
		assertEquals(response!=null,true);
		assertEquals(response.getErrors().isEmpty(),false);
		if(CollectionUtils.isEmpty(response.getErrors()))
		{
			for(String s:response.getErrors())
			{
				LOG.info("The error is :"+s);
				assertEquals(s.contains("OUT_OF_STOCK")||s.contains("Error"),true);
			}
		}
		
		
	}
	
	@Test
	public void orderListingTestForUnsuccessfullOrders()
	{
		List<Order> orders=(List<Order>) orderService.getOrderByOrderStatus(OrderStatus.UNSUCCESSFUL,1);
		if(!CollectionUtils.isEmpty(orders))
		{
			assertEquals(orders.get(0).getStatus().equals(OrderStatus.UNSUCCESSFUL),true);
		}
		
	}
	
	@Test
	public void orderListingTestForNewOrders()
	{
		List<Order> orders=(List<Order>) orderService.getOrderByOrderStatus(OrderStatus.NEW,1);
		if(!CollectionUtils.isEmpty(orders))
		{
			assertEquals(orders.get(0).getStatus().equals(OrderStatus.NEW),true);
		}
		
	}
	
	@Test
	public void orderListingTestForPlacedOrders()
	{
		List<Order> orders=(List<Order>) orderService.getOrderByOrderStatus(OrderStatus.PLACED,1);
		if(!CollectionUtils.isEmpty(orders))
		{
			assertEquals(orders.get(0).getStatus().equals(OrderStatus.PLACED),true);
		}
		
	}

}
