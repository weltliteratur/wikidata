package wikidata;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.Value;

public class Downloader implements EntityDocumentProcessor {

	static final String filterPropertyId = "P31"; // "instance of"
	static final Value filterValue = Datamodel.makeWikidataItemIdValue("Q5"); // "human"

	int itemsWithPropertyCount;
	int itemCount = 0;
	BufferedWriter buf;

	public Downloader(final BufferedWriter buf) {
		this.buf = buf;
		this.itemsWithPropertyCount = 0;
	}

	public static void main(String[] args) throws IOException {
		ExampleHelpers.configureLogging();

		final BufferedWriter buf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("wikidata_humans_walias.tsv"), "UTF-8"));

		final Downloader processor = new Downloader(buf);
		ExampleHelpers.processEntitiesFromWikidataDump(processor);
		processor.printStatus();
		buf.close();
	}


	public void processItemDocument(final ItemDocument itemDocument) {
		this.itemCount++;

		// find an instance of "human"
		if (itemDocument.hasStatementValue("P31", Datamodel.makeWikidataItemIdValue("Q5"))) {
			try {
				this.itemsWithPropertyCount++;

				final ItemIdValue itemId = itemDocument.getItemId();
				final MonolingualTextValue label = itemDocument.getLabels().get("en");
				if (label != null) {
					buf.write(csvEscape(itemId.getId()) + "\t" + csvEscape(label.getText()));
					// add aliases
					final List<MonolingualTextValue> aliases = itemDocument.getAliases().get("en");
					if (aliases != null) {
						for (final MonolingualTextValue alias : aliases) {
							buf.write("\t" + csvEscape(alias.getText()));
						}
					}
					
					buf.write("\n");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Print progress every 100,000 items:
		if (this.itemCount % 100000 == 0) {
			printStatus();
		}
	}

	/**
	 * Escapes a string for use in CSV. In particular, the string is quoted and
	 * quotation marks are escaped.
	 *
	 * @param string
	 *            the string to escape
	 * @return the escaped string
	 */
	private String csvEscape(String string) {
		if (string == null) {
			return "\"\"";
		} else {
			return "\"" + string.replace("[\t\r\n]", " ").replace("\"", "\"\"") + "\"";
		}
	}

	public void processPropertyDocument(PropertyDocument propertyDocument) {
		// Nothing to do
	}

	/**
	 * Prints the current status, time and entity count.
	 */
	public void printStatus() {
		System.out.println("Found " + this.itemsWithPropertyCount 
				+ " matching items after scanning " + this.itemCount
				+ " items.");

	}

}
