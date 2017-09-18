package wikidata;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import wikidata.WikidataUtils.PropertyValue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Combines data and writes JSON.
 * 
 * @author rja
 *
 */
public class JsonWriter {

	
	private final Map<String, Map<String, List<PropertyValue>>> items;
	private final Map<String, String> coordinates;
	
	public JsonWriter(final Map<String, Map<String, List<PropertyValue>>> items, final Map<String, String> coordinates) {
		super();
		this.items = items;
		this.coordinates = coordinates;
	}
	
	public void write(final String fileName) throws IOException {
		final JsonFactory factory = new JsonFactory();
		final JsonGenerator json = factory.createGenerator(new OutputStreamWriter(new FileOutputStream(fileName), "utf-8"));
		json.writeStartObject();
		writeItems(json);
		json.writeEndObject();
		json.close();		
	}
	
	private void writeItems(final JsonGenerator json) throws IOException {
		for (final Entry<String, Map<String, List<PropertyValue>>> entry : items.entrySet()) {
			json.writeFieldName(entry.getKey()); // "118540238" : 
			json.writeStartObject();             // {
			writeProperties(json, entry.getValue());
			json.writeEndObject();               // }
			json.writeRaw('\n');                 // add linebreak			
		}
	}
	
	/**
	 * 
	 * FIXME: add coordinates
	 * 
	 * @param json
	 * @param properties
	 * @throws IOException
	 */
	private void writeProperties(final JsonGenerator json, final Map<String, List<PropertyValue>> properties) throws IOException {
		for (final Entry<String, List<PropertyValue>> entry : properties.entrySet()) {
			json.writeFieldName(entry.getKey()); //   "occupations" :
			final List<PropertyValue> values = entry.getValue();
			if (values.size() == 1) {
				json.writeString(values.get(0).toString());
			} else {
				json.writeStartArray();
				for (final PropertyValue value : values) {
					// FIXME: debug
//					if ("Q1209498".equals(value.valueId)) {
//						System.out.println(this.getClass().getSimpleName() + ": found value with id " + value.valueId);
//					}

					json.writeString(value.toString());
				}
				json.writeEndArray();
			}
			
		}
	}
	
//
//	private void writeJson(final JsonGenerator json, final ItemIdValue itemId,
//			final MonolingualTextValue label, final Set<String> occupations,
//			final String gndid) throws IOException {
//		// write JSON
//		json.writeFieldName(gndid);         // "118540238" : 
//		json.writeStartObject();            // {
//		json.writeFieldName("id");          //   "id" :
//		json.writeString(itemId.getId());   //          "Q5879",
//		json.writeFieldName("name");        //   "name" :
//		json.writeString(label.getText());  //            "Johann Wolfgang von Goethe",
//		json.writeFieldName("occupations"); //   "occupations" : 
//		json.writeStartArray();             //     [
//		for (final String occupation : occupations) {
//			json.writeStartObject();        //        {
//			json.writeFieldName("id");      //          "id" :
//			json.writeString(occupation);   //                 "Q1209498",
//			json.writeFieldName("name");    //          "name" : 
//			json.writeString(this.subclasses.get(occupation)); // "poet lawyer"
//			json.writeEndObject();          //        }
//		}
//		json.writeEndArray();               //     ]
//		json.writeEndObject();              // }
//		json.writeRaw('\n');                // add linebreak
//	}


	
}
