package com.openedit.generators;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import org.openedit.generators.filters.TranslationFilter;

import com.openedit.BaseTestCase;
import com.openedit.page.Page;
import com.openedit.util.OutputFiller;

public class TextFilterTest extends BaseTestCase
{
	public void testTranslation() throws Exception
	{
		Page page = getFixture().getPageManager().getPage("/generators/translate.html");
		
		TranslationFilter filter = new TranslationFilter(page,"&locale=en");

		StringBuffer test = filter.replace("1 [[2]] [[3]]");
		assertEquals(test.toString(),"1 2 3");
		StringWriter out = new StringWriter();
		new OutputFiller().fill(filter, out);
		System.out.println(out.toString());
		
	}
}
