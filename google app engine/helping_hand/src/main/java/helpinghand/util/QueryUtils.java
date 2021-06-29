package helpinghand.util;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;

public class QueryUtils {
	
	private static final String DATASTORE_ERROR = "Error executing query: %s";
	private static final String TRANSACTION_ACTIVE_ERROR = "Error executing query: Transaction was active";
	
	
	
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final Logger log= Logger.getLogger(QueryUtils.class.getName());
	
	
	public static Entity getEntityById(String kind, long event) {
		
		Key key = datastore.newKeyFactory().setKind(kind).newKey(event);
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			Entity entity =  txn.get(key);
			return entity;
			
		}catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_ERROR, e.toString()));
			return null;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return null;
			}
		}
		
	}
	
	/**
	 * Queries the datastore for an Entity by its property
	 * @param kind - the kind of the entity
	 * @param property - the name of the property to check
	 * @param value - the value of the property to check against
	 * @return the Entity if it exists, null if it doesn't or in case of error
	 */
	public static Entity getEntityByProperty(String kind, String property, String value) {
		
		Query<Entity> query = Query.newEntityQueryBuilder().setKind(kind).setFilter(PropertyFilter.eq(property, value)).build();
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			QueryResults<Entity> results = txn.run(query);
			txn.commit();
			if(results.hasNext())return results.next();
			return null;
			
		}catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_ERROR, e.toString()));
			return null;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return null;
			}
		}

		
	}
	
	/**
	 * Queries the datastore for all entities y their property
	 * @param kind - the kind of the entity
	 * @param property - the name of the property to check
	 * @param value - the value of the property to check against
	 * @return a list of entities that have meet the requirements, or null if there is an error
	 */
	public static List<Entity> getEntityListByProperty(String kind, String property, String value) {
		
		Query<Entity> query = Query.newEntityQueryBuilder().setKind(kind).setFilter(PropertyFilter.eq(property, value)).build();
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			QueryResults<Entity> results = txn.run(query);
			txn.commit();
			
			List<Entity> entities = new LinkedList<>();
			results.forEachRemaining(entity->entities.add(entity));
			return entities;
			
		}catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_ERROR, e.toString()));
			return null;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return null;
			}
		}

		
	}
	
	public static List<Entity> getEntityListByProperty(String kind, String property, boolean value) {
		
		Query<Entity> query = Query.newEntityQueryBuilder().setKind(kind).setFilter(PropertyFilter.eq(property, value)).build();
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		
		try {
			QueryResults<Entity> results = txn.run(query);
			txn.commit();
			
			List<Entity> entities = new LinkedList<>();
			results.forEachRemaining(entity->entities.add(entity));
			return entities;
			
		}catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_ERROR, e.toString()));
			return null;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return null;
			}
		}

		
	}
	
	
	/**
	 * Queries the datastore for the child entities of an entity
	 * @param parent - the parent entity
	 * @return a list containing all children of the entity (can be empty), or null in case of error
	 */
	public static List<Entity> getEntityChildren(Entity parent) {
		
		if(parent == null)return null; 
		
		Query<Entity> query = Query.newEntityQueryBuilder().setFilter(PropertyFilter.hasAncestor(parent.getKey())).build();
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		List<Entity> children = new LinkedList<>();
		try {
			QueryResults<Entity> results = txn.run(query);
			txn.commit();
			results.forEachRemaining(child ->children.add(child));
			return children;
			
		}catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_ERROR, e.toString()));
			return null;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return null;
			}
		}

		
	}
	
	/**
	 * Queries the datastore for the child entities of an entity that are of a certain kind
	 * @param parent - the parent entity
	 * @return a list containing all children of the entity (can be empty), or null in case of error
	 */
	public static List<Entity> getEntityChildrenByKind(Entity parent,String kind) {
		
		if(parent == null)return null; 
		
		Query<Entity> query = Query.newEntityQueryBuilder().setKind(kind).setFilter(PropertyFilter.hasAncestor(parent.getKey())).build();
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		List<Entity> children = new LinkedList<>();
		try {
			QueryResults<Entity> results = txn.run(query);
			txn.commit();
			results.forEachRemaining(child ->children.add(child));
			return children;
			
		}catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_ERROR, e.toString()));
			return null;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return null;
			}
		}

		
	}
	
	/**
	 * Queries the datastore for the child entities of an entity that are of a certain kind and a satisfy a property check
	 * @param parent - the parent entity
	 * @return a list containing all children of the entity (can be empty), or null in case of error
	 */
	public static List<Entity> getEntityChildrenByKindAndProperty(Entity parent,String kind,String property,String value) {
		
		if(parent == null)return null; 
		
		Query<Entity> query = Query.newEntityQueryBuilder().setKind(kind).setFilter(CompositeFilter.and(PropertyFilter.eq(property, value),PropertyFilter.hasAncestor(parent.getKey()))).build();
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		List<Entity> children = new LinkedList<>();
		try {
			QueryResults<Entity> results = txn.run(query);
			txn.commit();
			results.forEachRemaining(child ->children.add(child));
			return children;
			
		}catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_ERROR, e.toString()));
			return null;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return null;
			}
		}

		
	}
	
public static List<Entity> getEntityChildrenByKindAndProperty(Entity parent,String kind,String property,boolean value) {
		
		if(parent == null)return null; 
		
		Query<Entity> query = Query.newEntityQueryBuilder().setKind(kind).setFilter(CompositeFilter.and(PropertyFilter.eq(property, value),PropertyFilter.hasAncestor(parent.getKey()))).build();
		Transaction txn = datastore.newTransaction(TransactionOptions.newBuilder().setReadOnly(ReadOnly.newBuilder().build()).build());
		List<Entity> children = new LinkedList<>();
		try {
			QueryResults<Entity> results = txn.run(query);
			txn.commit();
			results.forEachRemaining(child ->children.add(child));
			return children;
			
		}catch(DatastoreException e) {
			txn.rollback();
			log.severe(String.format(DATASTORE_ERROR, e.toString()));
			return null;
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				log.severe(TRANSACTION_ACTIVE_ERROR);
				return null;
			}
		}

		
	}
	
}
