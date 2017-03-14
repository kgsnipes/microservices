package com.codechallenge.services.impl;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.codechallenge.dto.PlaceOrderRequest;
import com.codechallenge.dto.PlaceOrderResponse;
import com.codechallenge.exception.InvalidProductException;
import com.codechallenge.exception.OutOfStockException;
import com.codechallenge.models.Order;
import com.codechallenge.models.OrderEntry;
import com.codechallenge.models.OrderStatus;
import com.codechallenge.models.Product;
import com.codechallenge.repository.OrderRepository;
import com.codechallenge.services.CalculationService;
import com.codechallenge.services.OrderService;
import com.codechallenge.services.ProductService;
import com.codechallenge.services.StockService;

public class DefaultOrderService implements OrderService {
	
	private static ReentrantLock lock=new ReentrantLock(true);
	
	@Autowired
	private ProductService productService;
	
	@Autowired
	private StockService stockService;
	
	@Autowired
	private OrderRepository orderRepository;
	
	@Autowired
	private CalculationService calculationService;
	
	
	@Override
	public PlaceOrderResponse placeOrder(final PlaceOrderRequest request) {
		
		PlaceOrderResponse response=new PlaceOrderResponse();
		try
		{/* first try for the lock on the monitor object and see if it returns immediately and if this fails then try for a timed lock to retain the fairness of a thread acquiring a lock*/
			if (lock.tryLock() || lock.tryLock(500, TimeUnit.MILLISECONDS) )
			{
				Order order=request.getOrder();
				checkProductAvailabilityAndReduceInventory(request,response);
				calculationService.calculateOrder(order);
				order.setStatus(OrderStatus.PLACED);
				order=saveOrder(order);
				response.setOrder(order);
				response.setStatus(OrderStatus.PLACED.toString());
			}
		}
		catch(Exception ex)
		{
			response.getErrors().add("Error :"+ex.getMessage());
			request.getOrder().setStatus(OrderStatus.UNSUCCESSFUL);
			saveOrder(request.getOrder());
		}
		finally
		{
			lock.unlock();
		}
		return response;
	}
	
	private void checkProductAvailabilityAndReduceInventory(final PlaceOrderRequest request,final PlaceOrderResponse response) throws Exception
	{
		Order order=request.getOrder();
		if(order!=null && order.getId()!=null)
		{
			if(order.getOrderEntries()!=null && !CollectionUtils.isEmpty(order.getOrderEntries()) )
			{
				for(OrderEntry entry:order.getOrderEntries())
				{
					try
					{
						Product product=productService.getProductById(entry.getProductId());
						Integer currentStock=stockService.getCurrentStockForProduct(product.getId());
						if(entry.getQty()<=0)
						{
							throw new Exception("Quantity is not provide in the order entry for the product :"+product.getName()+" in order :"+order.getId());
						}
						if(entry.getQty()>currentStock)
						{
							throw new OutOfStockException("OUT_OF_STOCK: The order entry for product requires quantity than available. Product: "+product.getName()+ " available: "+currentStock+ " required : "+entry.getQty());
						}
						else
						{
							stockService.reduceStockForProduct(product.getId(), entry.getQty());
						}
					}
					catch(InvalidProductException productException)
					{
						response.getErrors().add("Error :"+productException.getMessage());
					}
					catch(OutOfStockException outOfStock)
					{
						response.getErrors().add("Error :"+outOfStock.getMessage());
					}
				}
				
			}
			else
			{
				throw new Exception("No Order Entries in the order :"+request.getOrder().getId());
			}
		}
		else
		{
			throw new Exception("Please provide a valid order");
		}
		
		if(!CollectionUtils.isEmpty(response.getErrors()))
		{
			throw new Exception("There are errors in the order.");
		}
		
	}
	
	@Override
	@Transactional
	public Order saveOrder(Order order)
	{
		return orderRepository.save(order);
	}

	@Override
	@Cacheable(value="ordercache", key="#orderId")
	public Order getOrderByOrderId(Long orderId) {
		return orderRepository.findOne(orderId);
	}
	
	@Override
	@Transactional
	public Order createOrder(Order order) {
		order.setStatus(OrderStatus.NEW);
		return orderRepository.save(order);
	}

	@Override
	@Caching(
		      put = {
		            @CachePut(value = "ordercache", key = "#orderStatus.toString()+'_'+#page.toString()")
		      }
			)
	//@Cacheable(value="ordercache", key="#orderStatus.toString().append(#page.toString())")
	public Collection<Order> getOrderByOrderStatus(OrderStatus orderStatus,Integer page) {
		// TODO Auto-generated method stub
		return orderRepository.findByStatus(orderStatus,new PageRequest(page, 100));
	}

}
