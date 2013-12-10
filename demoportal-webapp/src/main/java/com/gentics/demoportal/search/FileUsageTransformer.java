package com.gentics.demoportal.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import com.gentics.cr.CRConfigUtil;
import com.gentics.cr.CRRequest;
import com.gentics.cr.CRResolvableBean;
import com.gentics.cr.RequestProcessor;
import com.gentics.cr.StaticRPContainer;
import com.gentics.cr.configuration.GenericConfiguration;
import com.gentics.cr.configuration.StaticConfigurationContainer;
import com.gentics.cr.exceptions.CRException;
import com.gentics.cr.lucene.indexer.transformer.ContentTransformer;
import com.gentics.cr.lucene.indexer.transformer.LuceneContentTransformer;

public class FileUsageTransformer extends ContentTransformer implements
		LuceneContentTransformer {
	private static Logger logger = Logger.getLogger(FileUsageTransformer.class);

	private static final String TRANSFORMER_LINKEDFILES_KEY = "attribute_linked_files";
	private static final String TRANSFORMER_LINKED_IN_PAGES_KEY = "attribute_linked_in_pages";
	private static final String TRANSFORMER_PATTERN_KEY = "pattern";
	private static final String TRANSFORMER_INDEXPART_KEY = "indexpart";
	private static final String TRANSFORMER_CONFIG_KEY = "config";
	private static final String METABEAN_CONTENTID = "10001";
	private String linkedFiles = "";
	private String linkedInPages = "";
	private String pattern = "";
	private String indexPart = "";
	private String conf = "";

	/**
	 * Creates instance of FileUsageTransformer
	 * 
	 * @param config
	 */
	public FileUsageTransformer(GenericConfiguration config) {
		super(config);
		linkedFiles = (String) config.get(TRANSFORMER_LINKEDFILES_KEY);
		linkedInPages = (String) config.get(TRANSFORMER_LINKED_IN_PAGES_KEY);
		pattern = (String) config.get(TRANSFORMER_PATTERN_KEY);
		indexPart = (String) config.get(TRANSFORMER_INDEXPART_KEY);
		conf = (String) config.get(TRANSFORMER_CONFIG_KEY);
	}

	@Override
	public void processBean(CRResolvableBean bean) {
	}

	public void processBean(CRResolvableBean bean, IndexWriter writer)
			throws CRException {
		if (indexPart != null) {

			long s = System.currentTimeMillis();
			logger.debug("index part: " + indexPart);
			if (indexPart.equals("CONTENT_PAGES")) {
				String content = bean.getContent();
				Pattern regex = Pattern.compile(pattern);
				Matcher m = regex.matcher(content);
				String newattr = " ";
				String docQuery = "";

				logger.debug("contentid: " + bean.getContentid());

				while (m.find()) {
					if (m.group(1) != null) {
						newattr += m.group(1) + " ";
						if ("".equals(docQuery)) {
							docQuery += "obj_type:10008 AND contentid:(10008."
									+ m.group(1);
						} else {
							docQuery += " 10008." + m.group(1);
						}
					}
				}

				if (!"".equals(docQuery)) {
					logger.debug("set attribute 'linkedfiles': " + newattr);
					if (!" ".equals(newattr))
						bean.set(linkedFiles, newattr);

					docQuery += ")";
					logger.debug("query to get files to delete: " + docQuery);
					try {
						StandardAnalyzer analyzer = new StandardAnalyzer(
								Version.LUCENE_CURRENT);
						Query q = new QueryParser(Version.LUCENE_CURRENT,
								"contentid", analyzer).parse(docQuery);
						writer.deleteDocuments(q);
					} catch (CorruptIndexException cie) {
						logger.error(cie);
					} catch (IOException ioe) {
						logger.error(ioe);
					} catch (Exception e) {
						logger.error(e);
					}
				} else {
					logger.debug("no linked files");
				}

			} else {
				String contentid = bean.getContentid();
				if (contentid.indexOf(".") != -1) {
					contentid = contentid.substring(contentid.indexOf(".") + 1);

					CRConfigUtil config = StaticConfigurationContainer
							.getConfig(conf, null);
					RequestProcessor rp = StaticRPContainer.getRP(config, 1);
					CRRequest rq = new CRRequest();
					logger.debug("contentid: " + contentid);
					rq.setRequestFilter("obj_type:10007 AND linkedfiles:\" "
							+ contentid + " \"");
					rq.set("secondary", "true");
					rq.set(RequestProcessor.META_RESOLVABLE_KEY, true);
					ArrayList<String> attributeArray = new ArrayList<String>();
					attributeArray.add("contentid");
					rq.setAttributeArray(attributeArray.toArray(new String[0]));
					Collection<CRResolvableBean> coll = rp.getObjects(rq);
					String liP = " ";

					if (coll != null) {
						Iterator<CRResolvableBean> it = coll.iterator();
						ArrayList<String> lsids = new ArrayList<String>();
						while (it.hasNext()) {
							CRResolvableBean b = it.next();
							String cid = b.getContentid();
							if (cid != METABEAN_CONTENTID) {
								String langSet = (String) b
										.get("languagesetid");
								logger.debug("cid: " + cid
										+ ", languagesetid: " + langSet);
								if (!lsids.contains(langSet)) {
									cid = cid.substring(cid.indexOf(".") + 1);
									liP += cid + " ";
									lsids.add(langSet);
								}
							}
						}
						if (!" ".equals(liP)) {
							bean.set(linkedInPages, liP);
							logger.debug("set attribute 'linkedinpages': "
									+ liP);
						}
					}
				}
			}
			long e = System.currentTimeMillis();
			logger.debug("duration: " + (e - s) + "ms");
		}
	}

	@Override
	public void destroy() {

	}
}
