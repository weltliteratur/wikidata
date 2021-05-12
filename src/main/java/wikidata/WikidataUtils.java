package wikidata;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementDocument;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

public class WikidataUtils {

	// to format months and days for dates
	private static final DecimalFormat TIME_FORMAT = new DecimalFormat("00");


	public static String getValue(final Value val) {
		if (val instanceof EntityIdValue) {
			return ((EntityIdValue)val).getId();
		}
		if (val instanceof StringValue) {
			return ((StringValue)val).getString();
		}
		if (val instanceof TimeValue) {
			final TimeValue time = (TimeValue)val;
			// FIXME: can we run into problems because we ignore the calendar?
			return time.getYear() + "-" + TIME_FORMAT.format(time.getMonth()) + "-" + TIME_FORMAT.format(time.getDay()); 
		}
		if (val instanceof GlobeCoordinatesValue) {
			final GlobeCoordinatesValue coord = (GlobeCoordinatesValue)val;
			// should work for Elastic, see https://www.elastic.co/guide/en/elasticsearch/guide/current/lat-lon-formats.html
			return coord.getLatitude() + ", " + coord.getLongitude(); 
		}
		return null;
	}

	//	public static String getValue(final ItemDocument doc, final String propertyId) {
	//		final StatementGroup stmts = doc.findStatementGroup(propertyId);
	//		final StringBuilder result = new StringBuilder();
	//		for (final Statement statement : stmts) {
	//			result.append(getValue(statement.getValue()) + " | ");
	//		}
	//		return result.toString();
	//	}

	/**
	 * Extracts the value from the statement. The value could be a literal or id
	 * and both cases are appropriately handled. In case of a literal, the id is
	 * set to null.
	 * 
	 * @param stmt
	 * @return
	 */
	public static PropertyValue getValue(final Statement stmt) {
		final Value val = stmt.getValue();
		if (val != null) {
			if (val instanceof EntityIdValue) {
				return new PropertyValue(((EntityIdValue)val).getId());
			}
			return new PropertyValue(null, getValue(val));
		}
		return null;
	}

	/**
	 * Retrieves all property values from the document and returns them as a set.  
	 * 
	 * @param doc
	 * @param propertyId
	 * @return
	 */
	public static List<PropertyValue> getValues(final StatementDocument doc, final String propertyId) {
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

	public static class PropertyValue implements Comparable<PropertyValue> {
		// ids of the value
		public final String valueId;
		// resolved value
		public String value = null;

		// default constructor: we know the id of the property but not its value 
		public PropertyValue(final String valueId) {
			this.valueId = valueId;
		}

		// when we already know the value
		public PropertyValue(final String valueId, final String value) {
			this.valueId = valueId;
			this.value = value;
		}


		@Override
		public boolean equals(Object obj) {
			if (this == obj) 
				return true;
			if (obj == null) 
				return false;
			if (getClass() != obj.getClass())
				return false;
			return valueId.equals(((PropertyValue) obj).valueId);
		}

		@Override
		public int hashCode() {
			return valueId.hashCode();
		}

		public int compareTo(final PropertyValue o) {
			return valueId.compareTo(o.valueId);
		}

		@Override
		public String toString() {
			if (value == null) {
				return valueId;
			}
			return value;
		}

	}

}
