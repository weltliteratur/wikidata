package wikidata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementDocument;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import wikidata.WikidataUtils.PropertyValue;

/**
 * Processes a Wikidata dump and extracts all entities with an occupation
 * property, a label, and a GND id together with the values for some of their
 * properties.
 * 
 * @author rja
 *
 */
public class WriterCollector implements EntityDocumentProcessor {

	public Map<String, Map<String, List<PropertyValue>>> getItems() {
		return items;
	}

	/*
	 * list of properties we are interested in:
	 * 
	 * | id    | name                                | round | note                    |
	 * |-------+-------------------------------------+-------+-------------------------|
	 * | P106  | occupation                          | 1+2   | condition for inclusion |
	 * | P227  | GND id                              | 1     | condition for inclusion |
	 * | P21   | gender                              | 2?    |                         |
	 * | P569  | date of birth                       | 1?    |                         |
	 * | P19   | place of birth                      | 2     |                         |
	 * | P625  | - coordinate location               | 2     |                         |
	 * | P570  | date of death                       | 1?    |                         |
	 * | P20   | place of death                      | 1?    |                         |
	 * | P625  | - coordinate location               | 2     |                         |
	 * | P103  | native language                     | 2     |                         |
	 * | P1412 | languages spoken, written or signed | 2     |                         |
	 * | P166  | awards received                     | 2     |                         |
	 * | P18   | image (P18)                         | 1?    |                         |
	 * | 
	 */
	private static final String PROPERTY_GNDID = "P227";
	private static final String PROPERTY_OCCUPATION = "P106";
	private static final Map<String, String> PROPERTIES = new HashMap<String, String>();
	static {
		PROPERTIES.put(PROPERTY_OCCUPATION, "occupation");
		PROPERTIES.put("P21", "gender");
		PROPERTIES.put("P569", "date_of_birth");
		PROPERTIES.put("P19", "place_of_birth");
		PROPERTIES.put("P570", "date_of_death");
		PROPERTIES.put("P20", "place_of_death");
		PROPERTIES.put("P103", "native_language");
		PROPERTIES.put("P1412", "languages");
		PROPERTIES.put("P166", "awards");
		PROPERTIES.put("P18", "image");
	}

	// to extract two-character language identifier
	private static final Pattern WIKILANG = Pattern.compile("^([a-z][a-z])wiki$");
	
	/*
	 * each item has many properties and each property can have several values
	 */
	private final Map<String, Map<String, List<PropertyValue>>> items = new LinkedHashMap<String, Map<String, List<PropertyValue>>>();
	/*
	 * property values whose labels must be resolved
	 */
	private final Map<String, PropertyValue> valuesWithMissingLabels = new HashMap<String, PropertyValue>();


	public Map<String, PropertyValue> getValuesWithMissingLabels() {
		return valuesWithMissingLabels;
	}

	public void processItemDocument(final ItemDocument itemDocument) {
		/*
		 * check for occupation (P106) and GND id (P227) properties
		 */
		if (itemDocument.hasStatement(PROPERTY_OCCUPATION) && itemDocument.hasStatement(PROPERTY_GNDID)) {
			// extract id
			final String itemId = itemDocument.getEntityId().getId();
			// FIXME: debug
			if ("Q1209498".equals(itemId)) {
				System.out.println(this.getClass().getSimpleName() + ": found item with id " + itemId + " and " + itemDocument.getStatementGroups().size() + " statements");
			}
			
			// extract label
			final MonolingualTextValue label = itemDocument.getLabels().get("en");
			// ignore items without label
			if (label != null) {
				/*
				 * an item can have several GND ids (example: https://www.wikidata.org/wiki/Q19004)
				 * - get and print them all 
				 */
				for (final Statement statement : itemDocument.findStatementGroup(PROPERTY_GNDID)) {
					// get GND id
					final String gnd = WikidataUtils.getValue(statement.getValue());
					// ignore empty GNDs
					if (gnd != null ) {
						// have found valid item -> create entry for it
						final HashMap<String, List<PropertyValue>> properties = new HashMap<String, List<PropertyValue>>();
						// map GND id to properties
						this.items.put(gnd, properties);
						// add Wikidata id
						properties.put("id", Collections.singletonList(new PropertyValue("", itemId)));
						// add label
						properties.put("name", Collections.singletonList(new PropertyValue("", label.getText())));
						// collect remaining properties
						for (final Entry<String, String> prop : PROPERTIES.entrySet()) {
							// get all values
							final List<PropertyValue> values = getValues(itemDocument, prop.getKey());
							if (!values.isEmpty()) {
								// add to set
								properties.put(prop.getValue(), values);
							}
						}
						// get sitelinks
						final List<PropertyValue> siteKeys = new ArrayList<PropertyValue>(itemDocument.getSiteLinks().size());
						for (final SiteLink siteLink : itemDocument.getSiteLinks().values()) {
							final String siteKey = siteLink.getSiteKey();
							final Matcher matcher = WIKILANG.matcher(siteKey);
							if (matcher.matches()) {
								siteKeys.add(new PropertyValue(null, matcher.group(1)));
							}
						}
						properties.put("sitelinks", siteKeys);
					}

					//					
					//					
					//					try {
					//						//writeJson(itemId, label, occupations, gndid);
					//
					//						/*
					//						 * | id    | name                                | round | note                    |
					//						 * |-------+-------------------------------------+-------+-------------------------|
					//						 * | P106  | occupation                          | 1+2   | condition for inclusion |
					//						 * | P227  | GND id                              | 1     | condition for inclusion |
					//						 * | P21   | gender                              | 2?    |                         |
					//						 * | P569  | date of birth                       | 1?    |                         |
					//						 * | P19   | place of birth                      | 2     |                         |
					//						 * | P625  | - coordinate location               | 2     |                         |
					//						 * | P570  | date of death                       | 1?    |                         |
					//						 * | P20   | place of death                      | 1?    |                         |
					//						 * | P625  | - coordinate location               | 2     |                         |
					//						 * | P103  | native language                     | 2     |                         |
					//						 * | P1412 | languages spoken, written or signed | 2     |                         |
					//						 * | P166  | awards received                     | 2     |                         |
					//						 * | P18   | image (P18)                         | 1?    |                         |
					//						 */
					//						// debug: Goethe
					//						if (itemId.getId().equals("Q5879")) {
					//							System.out.println("gender: " + WikidataUtils.getValue(itemDocument, "P21"));
					//							System.out.println("datofb: " + WikidataUtils.getValue(itemDocument, "P569"));
					//							System.out.println("plaofb: " + WikidataUtils.getValue(itemDocument, "P19"));
					//							System.out.println("datofd: " + WikidataUtils.getValue(itemDocument, "P570"));
					//							System.out.println("plaofd: " + WikidataUtils.getValue(itemDocument, "P20"));
					//							System.out.println("natlan: " + WikidataUtils.getValue(itemDocument, "P103"));
					//							System.out.println("wrilan: " + WikidataUtils.getValue(itemDocument, "P1412"));
					//							System.out.println("awards: " + WikidataUtils.getValue(itemDocument, "P166"));
					//							System.out.println("images: " + WikidataUtils.getValue(itemDocument, "P18"));
					//
					//						}
					//
					//					} catch (final Exception e) {
					//						System.err.println("error for " + itemId + "(" + label + ")");
					//						e.printStackTrace();
					//					}
				} 
			}
		}
	}
	
	/**
	 * Retrieves all property values from the document and returns them as a set.  
	 * 
	 * @param doc
	 * @param propertyId
	 * @return
	 */
	private List<PropertyValue> getValues(final StatementDocument doc, final String propertyId) {
		final List<PropertyValue> result = new LinkedList<WikidataUtils.PropertyValue>();

		final StatementGroup stmts = doc.findStatementGroup(propertyId);
		if (stmts != null) { 
			for (final Statement statement : stmts) {
				final PropertyValue value = getValue(statement);
				if (value != null) {
					result.add(value);
				}
			}
		}
		return result;
	}
	
	/**
	 * Extracts the value from the statement. The value could be a literal or id
	 * and both cases are appropriately handled. In case of a literal, the id is
	 * set to null.
	 * 
	 * @param stmt
	 * @return
	 */
	private PropertyValue getValue(final Statement stmt) {
		final Value val = stmt.getValue();
		if (val != null) {
			if (val instanceof EntityIdValue) {
				return createPropertyValue(((EntityIdValue)val).getId(), null);
			}
			return createPropertyValue(null, WikidataUtils.getValue(val));
		}
		return null;
	}

	/**
	 * Adds the value to the set of missing values, or returns the existing object for that value.
	 * 
	 * @param valueId
	 * @param value
	 * @return
	 */
	private PropertyValue createPropertyValue(final String valueId, final String value) {
		if (valueId != null) {
			if (this.valuesWithMissingLabels.containsKey(valueId)) {
				return this.valuesWithMissingLabels.get(valueId);
			}
			final PropertyValue propertyValue = new PropertyValue(valueId);
			this.valuesWithMissingLabels.put(valueId, propertyValue);
			return propertyValue;
		}
		return new PropertyValue(null, value);
	}
	
	
	public void processPropertyDocument(PropertyDocument arg0) {
		// TODO Auto-generated method stub

	}
}