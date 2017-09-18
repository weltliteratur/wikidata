package wikidata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;

import wikidata.WikidataUtils.PropertyValue;

/**
 * Given a list of entities, retrieves their labels.
 * 
 * @author rja
 *
 */
public class PropertyValueCollector implements EntityDocumentProcessor {

	/*
	 * property values whose labels must be resolved
	 */
	private final Map<String, PropertyValue> valuesWithMissingLabels;
	// to collect coordinates
	private final Map<String, String> coordinates;

	public Map<String, String> getCoordinates() {
		return coordinates;
	}

	public PropertyValueCollector(final Map<String, PropertyValue> valuesWithMissingLabels) {
		this.valuesWithMissingLabels = valuesWithMissingLabels;
		this.coordinates = new HashMap<String, String>();
	}

	public void processItemDocument(final ItemDocument itemDocument) {
		// get id
		final String itemId = itemDocument.getItemId().getId();

		// check if we need to get the label for that id
		final PropertyValue val = this.valuesWithMissingLabels.get(itemId);
		if (val != null) {
			// found! get label
			final MonolingualTextValue label = itemDocument.getLabels().get("en");
			if (label != null) {
				// set label
				val.value = label.getText();
			}
			// check if this item has coordinates (P625)
			final List<PropertyValue> values = WikidataUtils.getValues(itemDocument, "P625");
			if (!values.isEmpty()) {
				coordinates.put(itemId, values.get(0).value);
			}
		}

	}

	public void processPropertyDocument(final PropertyDocument propDocument) {
		// noop
	}
}
