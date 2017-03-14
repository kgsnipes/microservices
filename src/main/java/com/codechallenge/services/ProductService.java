package com.codechallenge.services;

import java.util.List;

import com.codechallenge.exception.InvalidProductException;
import com.codechallenge.models.Product;
/* this interface defines the methods for the product related operations required for this application */
public interface ProductService {
	
	public Product getProductById(Long id)throws InvalidProductException;
	
	public Product getProductByName(String name)throws InvalidProductException;
	
	public List<Product> getAllProducts();
	
	public void removeProducts(List<Product> products);
	
	public Product createProduct(Product product);
	
	public Product getProductRealTimeById(Long id)throws InvalidProductException;

}
