package wikidata;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import wikidata.WikidataUtils.PropertyValue;

/**
 * Reads all subclasses (P279) of writer (Q36180) from a file and then iterates
 * over the Wikidata dataset to find all items which have a GND id (P227) and an
 * occupation (P106) of one of the subclasses.
 * 
 * @author rja
 *
 */
public class WriterExtractor {

	public static void main(String[] args) throws IOException {
		ExampleHelpers.configureLogging();

		final String basedir = "./";

		// TODO: store whether entity has writer occupation
		final String subclasses = basedir + "wikidata_writer_subclasses.tsv";


		final String outputfileName = basedir + "gnditems_" + new SimpleDateFormat("YYYY-MM-dd_HH:mm").format(new Date()) + ".json";


		// collect all entities that have a GND id, a label, and an occupation property 
		final WriterCollector writerCollector = new WriterCollector();
		ExampleHelpers.processEntitiesFromWikidataDump(writerCollector);

		final Map<String, Map<String, List<PropertyValue>>> items = writerCollector.getItems();
		final Map<String, PropertyValue> valuesWithMissingLabels = writerCollector.getValuesWithMissingLabels();
		System.out.println("read " + items.size() + " items and " + valuesWithMissingLabels.size() + " property values with missing labels");

		// collect the labels of some of the properties
		final PropertyValueCollector propertyValueCollector = new PropertyValueCollector(valuesWithMissingLabels);
		ExampleHelpers.processEntitiesFromWikidataDump(propertyValueCollector);

		final Map<String, String> coordinates = propertyValueCollector.getCoordinates();
		System.out.println("read " + coordinates.size() + " coordinates");
		System.out.println(countMissing(valuesWithMissingLabels) + " of " + valuesWithMissingLabels.size() + " still missing");

		// add writer occupations
		final Map<String, String> writerSubclasses = getSubclasses(subclasses);
		addWriterOccupations(items, writerSubclasses);

		// print json
		final JsonWriter jsonWriter = new JsonWriter(items, coordinates);
		jsonWriter.write(outputfileName);

	}

	private static void addWriterOccupations(final Map<String, Map<String, List<PropertyValue>>> items, final Map<String, String> writerSubclasses) {
		for (final Entry<String, Map<String, List<PropertyValue>>> entry : items.entrySet()) {
			final LinkedList<PropertyValue> writerOccupations = new LinkedList<PropertyValue>(); 
			final Map<String, List<PropertyValue>> properties = entry.getValue();
			if (properties.containsKey("occupation")) {
				for (final PropertyValue occup : properties.get("occupation")) {
					if (writerSubclasses.containsKey(occup.valueId)) {
						writerOccupations.add(new PropertyValue(occup.valueId, writerSubclasses.get(occup.valueId)));
					}
				}
				if (!writerOccupations.isEmpty()) {
					properties.put("occupation_writer", writerOccupations);
				}
			}
		}
	}

	private static int countMissing(final Map<String, PropertyValue> props) {
		int count = 0;
		for (final Entry<String, PropertyValue> entry : props.entrySet()) {
			if (entry.getKey().equals(entry.getValue().toString())) {
				count += 1;
			}
		}
		return count;
	}


	private final Map<String, String> subclasses;

	// matches <http://www.wikidata.org/entity/Q36180>
	private static final Pattern WD_ID_PATTERN = Pattern.compile("^<.+/(Q[0-9]+)>$"); 
	// matches "writer"@en
	private static final Pattern WD_LABEL_PATTERN = Pattern.compile("^\"(.+)\"@en$");

	public WriterExtractor(final String subclassesFileName) throws IOException {
		this.subclasses = getSubclasses(subclassesFileName);
	}



	/**
	 * For a document that has the occupation (P106) property, checks all values
	 * against this.subclasses. All found matches are returned.
	 * 
	 * @param itemDocument
	 * @return
	 */
	private Set<String> getWriterOccupations(final ItemDocument itemDocument) {
		final Set<String> writerOccupations = new HashSet<String>();
		// extract occupations - since an item can have several values, we retrieve the statement group
		for (final Statement statement : itemDocument.findStatementGroup("P106").getStatements()) {
			final String occup = WikidataUtils.getValue(statement.getValue());
			if (this.subclasses.containsKey(occup)) {
				writerOccupations.add(occup);
			}
		}
		return writerOccupations;
	}

	/**
	 * For the given Wikidata id, read all subclasses (P279) of that class and
	 * return a map from the class id to the class label.
	 * 
	 * @param item
	 * @throws IOException
	 */
	public static Map<String, String> getSubclasses(final String fileName) throws IOException {
		final BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "utf-8"));
		final Map<String, String> subclasses = new HashMap<String, String>();
		String line;
		while ((line = buf.readLine()) != null) {
			/*
			 * input file format:
			 * ?subclass       ?subclassLabel
			 * <http://www.wikidata.org/entity/Q36180> "writer"@en
			 * <http://www.wikidata.org/entity/Q28389> "screenwriter"@en
			 * <http://www.wikidata.org/entity/Q49757> "poet"@en
			 * 
			 */
			if (line.startsWith("<")) {
				final String[] parts = line.trim().split("\t");
				final Matcher me = WD_ID_PATTERN.matcher(parts[0]);
				if (me.matches()) {
					final String id = me.group(1);
					final Matcher ml = WD_LABEL_PATTERN.matcher(parts[1]);
					final String label;
					if (ml.matches()) {
						label = ml.group(1);
					} else {
						label = id;
					}
					subclasses.put(id, label);
				}
			}
		}
		buf.close();
		return subclasses;
	}

}
