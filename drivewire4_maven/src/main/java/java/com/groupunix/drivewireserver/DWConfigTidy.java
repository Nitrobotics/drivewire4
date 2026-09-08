package com.groupunix.drivewireserver;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * wb 2026-09-07: commons-configuration 1.6 keeps the whitespace text nodes of the XML it loaded and
 * its transformer indents the tree again on every save, so config.xml and drivewireUI.xml grew by
 * thousands of blank lines per session (558 KB / 30,000 lines seen, 29,400 of them blank, and every
 * autosave rewrote the lot).  Stripping the whitespace-only text nodes from the live DOM right after
 * load makes every later save come out freshly indented with no accumulation.  Elements, attributes
 * and values are untouched.
 */
public class DWConfigTidy
{
	private static final Logger logger = Logger.getLogger("DWServer.DWConfigTidy");

	public static void tidy(XMLConfiguration cfg, String what)
	{
		if (cfg == null)
			return;

		Document doc = cfg.getDocument();

		if (doc == null)
			return;

		int removed = stripWhitespace(doc);

		if (removed > 0)
			logger.debug("tidied " + what + ": removed " + removed + " whitespace-only text node(s)");
	}

	public static int stripWhitespace(Node node)
	{
		int removed = 0;
		NodeList kids = node.getChildNodes();

		for (int i = kids.getLength() - 1; i >= 0; i--)
		{
			Node k = kids.item(i);

			if (k.getNodeType() == Node.TEXT_NODE)
			{
				String t = k.getNodeValue();

				if ((t == null) || (t.trim().length() == 0))
				{
					node.removeChild(k);
					removed++;
				}
				else if ((kids.getLength() == 1) && !t.equals(t.trim()))
				{
					// a leaf value with the old layout's line break and indent still attached (the value followed by a line break and a tab);
					// the reader trims it anyway, so store it trimmed
					k.setNodeValue(t.trim());
					removed++;
				}
			}
			else if (k.getNodeType() == Node.ELEMENT_NODE)
			{
				removed += stripWhitespace(k);
			}
		}

		return removed;
	}
}
