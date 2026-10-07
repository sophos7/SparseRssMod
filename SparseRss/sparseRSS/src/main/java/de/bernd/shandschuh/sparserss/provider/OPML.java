/**
 * Sparse rss
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package de.bernd.shandschuh.sparserss.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Xml;
import de.bernd.shandschuh.sparserss.Strings;

public class OPML {
	private static final String START = "<?xml version=\"1.0\" encoding=\"utf-8\"?><opml version=\"1.1\"><head><title>Sparse RSS export</title><dateCreated>";
	
	private static final String AFTERDATE = "</dateCreated></head><body>";
	
	private static final String OUTLINE_TITLE = "<outline title=\"";
	
	private static final String OUTLINE_XMLURL = "\" type=\"rss\" xmlUrl=\"";
	
	private static final String ATTRIBUTE_CATEGORY_VALUE = "/"+FeedData.FeedColumns.WIFIONLY;
	
	private static final String OUTLINE_CATEGORY = "\" category=\"";
	
	private static final String OUTLINE_CLOSING = "\" />";
	
	private static final String CLOSING = "</body></opml>\n";
	
	private static OPMLParser parser = new OPMLParser();
	
	public static void importFromStream(InputStream inputStream, Context context) throws IOException, SAXException {
		parser.context = context;
		parser.bodyTagEntered = false;
		parser.probablyValidElement = false;
		Xml.parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8), parser);
	}
	
	public static void exportToStream(OutputStream outputStream, Context context) throws IOException {
		Cursor cursor = context.getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, new String[] {FeedData.FeedColumns._ID, FeedData.FeedColumns.NAME, FeedData.FeedColumns.URL, FeedData.FeedColumns.WIFIONLY}, null, null, null);
		
		try {
			writeData(outputStream, cursor);
		} finally {
			cursor.close();
		}
	}
	
	private static void writeData(OutputStream outputStream, Cursor cursor) throws IOException {
		StringBuilder builder = new StringBuilder(START);
		
		builder.append(System.currentTimeMillis());
		builder.append(AFTERDATE);
		
		while(cursor.moveToNext()) {
			builder.append(OUTLINE_TITLE);
			builder.append(cursor.isNull(1) ? Strings.EMPTY : TextUtils.htmlEncode(cursor.getString(1)));
			builder.append(OUTLINE_XMLURL);
			builder.append(TextUtils.htmlEncode(cursor.getString(2)));
			if (cursor.getInt(3) == 1) {
				builder.append(OUTLINE_CATEGORY);
				builder.append(ATTRIBUTE_CATEGORY_VALUE);
			}
			builder.append(OUTLINE_CLOSING);
		}
		builder.append(CLOSING);
		
		Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

		writer.write(builder.toString());
		writer.flush();
	}
	
	private static class OPMLParser extends DefaultHandler {
		private static final String TAG_BODY = "body";
		
		private static final String TAG_OUTLINE = "outline";
		
		private static final String ATTRIBUTE_TITLE = "title";
		
		private static final String ATTRIBUTE_XMLURL = "xmlUrl";
		
		private static final String ATTRIBUTE_CATEGORY = "category";
		
		private boolean bodyTagEntered;
		
		private boolean probablyValidElement = false;
		
		private Context context;
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (!bodyTagEntered) {
				if (TAG_BODY.equals(localName)) {
					bodyTagEntered = true;
					probablyValidElement = true;
				}
			} else if (TAG_OUTLINE.equals(localName)) {
				String url = attributes.getValue(Strings.EMPTY, ATTRIBUTE_XMLURL);
				
				if (url != null) {
					String title = attributes.getValue(Strings.EMPTY, ATTRIBUTE_TITLE);
					
					ContentValues values = new ContentValues();
					
					values.put(FeedData.FeedColumns.URL, url);
					values.put(FeedData.FeedColumns.NAME, title != null && title.length() > 0 ? title : null);
					values.put(FeedData.FeedColumns.WIFIONLY, ATTRIBUTE_CATEGORY_VALUE.equals(attributes.getValue(Strings.EMPTY, ATTRIBUTE_CATEGORY)) ? 1 : 0);
					
					Cursor cursor = context.getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, null, new StringBuilder(FeedData.FeedColumns.URL).append(Strings.DB_ARG).toString(), new String[] {url}, null);
					
					if (!cursor.moveToFirst()) {
						context.getContentResolver().insert(FeedData.FeedColumns.CONTENT_URI, values);
					}
					cursor.close();
				}
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (bodyTagEntered && TAG_BODY.equals(localName)) {
				bodyTagEntered = false;
			}
		}

		@Override
		public void endDocument() throws SAXException {
			if (!probablyValidElement) {
				throw new SAXException();
			} else {
				super.endDocument();
			}
		}
		
		
		
	}
}
