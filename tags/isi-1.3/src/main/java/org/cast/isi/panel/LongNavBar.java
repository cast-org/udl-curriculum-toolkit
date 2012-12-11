/*
 * Copyright 2011 CAST, Inc.
 *
 * This file is part of the UDL Curriculum Toolkit:
 * see <http://code.google.com/p/udl-curriculum-toolkit>.
 *
 * The UDL Curriculum Toolkit is free software: you can redistribute and/or
 * modify it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The UDL Curriculum Toolkit is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cast.isi.panel;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.cast.cwm.components.ClassAttributeModifier;
import org.cast.cwm.data.Role;
import org.cast.cwm.data.User;
import org.cast.cwm.indira.IndiraImage;
import org.cast.cwm.indira.IndiraImageComponent;
import org.cast.cwm.xml.XmlSection;
import org.cast.cwm.xml.component.XmlComponent;
import org.cast.isi.ISIApplication;
import org.cast.isi.ISISession;
import org.cast.isi.ISIXmlSection;
import org.cast.isi.data.ContentLoc;
import org.cast.isi.page.ISIStandardPage;
import org.cast.isi.page.SectionLinkFactory;
import org.cast.isi.service.IISIResponseService;
import org.cast.isi.service.ISectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Deprecated in favor of implementing your own Nav Bar in the application that extends
 * {@link AbstractNavBar}.
 * 
 * @author jbrookover
 *
 */
@SuppressWarnings("serial")
@Deprecated
public class LongNavBar extends AbstractNavBar<XmlSection> {
	
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(LongNavBar.class);
	private List<String> locsWithUnread;
	private List<String> locsWithMessages;
	
	@Inject
	private ISectionService sectionService;

	@Inject
	protected IISIResponseService responseService;
	
	public LongNavBar(String id, XmlSection sec) {
		this(id, sec, false);
	}
	
	public LongNavBar(String id, final XmlSection sec, final boolean teacher) {
		super(id);
		final XmlSection top = sec.getAncestor("level1");
		final XmlSection currentLev2 = sec.getAncestor("level2");
		final String currentLev2Id = currentLev2 == null ? null : currentLev2.getId();
		final XmlSection currentLev3 = sec.getAncestor("level3");
		final String currentLev3Id = currentLev3 == null ? null : currentLev3.getId();
		final XmlSection currentLev4 = sec.getAncestor("level4");
		final String currentLev4Id = currentLev4 == null ? null : currentLev4.getId();
		
		User student = ISISession.get().getTargetUserModel().getObject();
		locsWithUnread = responseService.getPagesWithNotes(student, true);
		locsWithMessages = responseService.getPagesWithNotes(student);
				
		add (new Label("title", top.getTitle()));
		add (new ListView<XmlSection> ("lev2", top.getChildren()) {

			// Here we are iterating over the parts of the chapter: consider, investigate, ...
			protected void populateItem(ListItem<XmlSection> item) {
				final XmlSection sec2 = (XmlSection) item.getModelObject();
				
				if (sec2.hasChildren()) {				
					// This part has sections in it
					item.add(new Label("title", sec2.getTitle().toUpperCase()));
					final WebMarkupContainer container = new WebMarkupContainer("container");
					item.add(container);
					// FIXME set class "first"
					
					container.add(new ListView<XmlSection> ("lev3", sec2.getChildren()) {

						// Create icon for each section
						protected void populateItem(ListItem<XmlSection> item) {
							XmlSection sec3 = (XmlSection) item.getModelObject();
							boolean lev3isCurrent = sec3.getId().equals(currentLev3Id);
							BookmarkablePageLink link = new SectionLinkFactory().linkToPage("link", sec3);
							item.add(link);
							WebComponent icon = ISIApplication.get().iconFor(sec3, "_small_t");
							if (((ISIXmlSection) sec3).hasResponseGroup() && !ISISession.get().getUser().getRole().equals(Role.STUDENT))
								icon.add(new ClassAttributeModifier("hasResponseImg"));
							link.add(icon);

							if (lev3isCurrent) {
								// This is the currently selected section
								item.add(new ClassAttributeModifier(sec3.getClassName()));
								
								List<XmlSection> pageList = sec3.getChildren();
								if (pageList == null || pageList.isEmpty()) {
									pageList = new ArrayList<XmlSection>();
									pageList.add(sec3);
								}

								// Generate links to children
								item.add(new ListView<XmlSection> ("lev4", pageList) {
									protected void populateItem(ListItem<XmlSection> item) {
										XmlSection sec4 = (XmlSection) item.getModelObject();
										boolean isCurrent = sec4.getId().equals(currentLev4Id) || sec4.getId().equals(currentLev3Id);
										BookmarkablePageLink link = new SectionLinkFactory().linkToPage("link", sec4);
										item.add(link);
										if (isCurrent) {
											link.setEnabled(false);
											link.add(new ClassAttributeModifier("current"));
										}
										link.add(new Label("pagenum", String.valueOf(item.getIndex()+1)));
										item.setRenderBodyOnly(true);
										
										// Check to see if this page has any messages (read or unread)
										String location = (new ContentLoc(sec4)).getLocation();
										if (locsWithUnread.contains(location)) {
											link.add(new Image("messageIcon", new ResourceReference("img/icons/envelope_new.png")));
										} else if (locsWithMessages.contains(location)) {
											link.add(new Image("messageIcon", new ResourceReference("img/icons/envelope_old.png")));
										} else {
											link.add(new WebMarkupContainer("messageIcon").setVisible(false));
										}
										WebMarkupContainer hasResponse = new WebMarkupContainer("hasResponse");
										hasResponse.setVisible(((ISIXmlSection) sec4).hasResponseGroup() && !ISISession.get().getUser().getRole().equals(Role.STUDENT));
										link.add(hasResponse);
										
									}					
								});
								
								// Generate Next/Previous Links
								// Next / Previous Page Buttons
								BookmarkablePageLink previous = getNextPreviousLink("previous", currentLev4 == null ? null : currentLev4.getPrev(), "previous");
								BookmarkablePageLink next = getNextPreviousLink("next", currentLev4 == null ? null : currentLev4.getNext(), "next");
								
								item.add(previous);
								item.add(next);
								
								AjaxFallbackLink markReviewedLink = new AjaxFallbackLink("markReviewedLink") {

									private static final long serialVersionUID = 1L;

									@Override
									public void onClick(final AjaxRequestTarget target) {
										sectionService.toggleReviewed(ISISession.get().getStudent(), new ContentLoc(sec));
									
										
										if (target != null) {
											getPage().visitChildren(AbstractNavBar.class, new IVisitor<AbstractNavBar<XmlComponent>>() {
												public Object component(AbstractNavBar<XmlComponent> component) {
													target.addComponent(component);
													return CONTINUE_TRAVERSAL;
												}
											});
										}
									}
									
									@Override
									protected void onBeforeRender() {
										if (ISISession.get().getStudentModel() == null) {
											this.setVisible(false);
										}
										super.onBeforeRender();
									}
									
								};
								markReviewedLink.setOutputMarkupPlaceholderTag(true);
								IndiraImageComponent markReviewedButton = new IndiraImageComponent("markReviewedButton", new AbstractReadOnlyModel<IndiraImage>() {

									@Override
									public IndiraImage getObject() {
										boolean reviewed = sectionService.sectionIsReviewed(ISISession.get().getStudent(), (ISIXmlSection) sec);
										if (!reviewed) 
											return IndiraImage.get("img/buttons/mark_reviewed_off.png");
										else
											return IndiraImage.get("img/buttons/mark_reviewed_on.png");
									}
									
								});
								
								markReviewedLink.add(markReviewedButton);
								markReviewedLink.setOutputMarkupPlaceholderTag(true);
								// TODO: Should this always be the last page?
								if (teacher && ISISession.get().getStudent() != null 
										&& sectionService.sectionIsCompleted(ISISession.get().getStudent(), (ISIXmlSection) sec)) {
									markReviewedLink.setVisible(true);
								} else {
									markReviewedLink.setVisible(false);
								}
								item.add(markReviewedLink);

							} else {
								// Not currently selected; hide children and navigation arrows
								WebMarkupContainer lev4 = new WebMarkupContainer("lev4");
								lev4.setVisible(false);
								item.add(lev4);
								lev4.add(new WebMarkupContainer("link")
									.add(new WebMarkupContainer("pagenum")));
									
								item.add(new WebMarkupContainer("previous").setVisible(false))
									.add(new WebMarkupContainer("next").setVisible(false))
									.add(new WebMarkupContainer("markReviewedLink").setVisible(false));
							}
							
						}					
					});
					
				} else {
					// Part with no children: should be an overview section
					boolean isCurrent = sec2.getId().equals(currentLev2Id);
					
					item.add(new WebMarkupContainer("title").setVisible(false));
					
					WebMarkupContainer container = new WebMarkupContainer("container");
					container.add(new ClassAttributeModifier("notitle"));
					item.add(container);
					
					WebMarkupContainer lev3 = new WebMarkupContainer("lev3");
					container.add(lev3);
					if (isCurrent)
						lev3.add(new ClassAttributeModifier(sec2.getClassName()));

					BookmarkablePageLink link = new SectionLinkFactory().linkToPage("link", sec2);
					lev3.add(link);			
					link.add (ISIApplication.get().iconFor(sec2, "_small_t"));

					lev3.add(getNextPreviousLink("previous", null, "previous").setVisible(isCurrent));
					lev3.add(getNextPreviousLink("next", null, "next").setVisible(isCurrent));
					
					AjaxFallbackLink markReviewedLink = new AjaxFallbackLink("markReviewedLink") {

						private static final long serialVersionUID = 1L;

						@Override
						public void onClick(final AjaxRequestTarget target) {
							sectionService.toggleReviewed(ISISession.get().getStudent(), new ContentLoc(sec2));
						
							if (target != null) {
								getPage().visitChildren(AbstractNavBar.class, new IVisitor<AbstractNavBar>() {
									public Object component(AbstractNavBar component) {
										target.addComponent(component);
										return CONTINUE_TRAVERSAL;
									}
								});
							}
						}
						
						@Override
						protected void onBeforeRender() {
							if (ISISession.get().getStudentModel() == null) {
								this.setVisible(false);
							}
							super.onBeforeRender();
						}
						
					};
					markReviewedLink.setOutputMarkupPlaceholderTag(true);
					Image markReviewedButton = new Image("markReviewedButton", new AbstractReadOnlyModel() {
						
						private static final long serialVersionUID = 1L;

						@Override
						public Object getObject() {
							boolean reviewed = sectionService.sectionIsReviewed(ISISession.get().getStudent(), (ISIXmlSection) sec2);
							if (!reviewed) 
								return new ResourceReference("img/buttons/mark_reviewed_off.png");
							else
								return new ResourceReference("img/buttons/mark_reviewed_on.png");
						}
						
					});
					markReviewedLink.add(markReviewedButton);
					markReviewedLink.setOutputMarkupPlaceholderTag(true);

					if (isCurrent && teacher && ISISession.get().getStudent() != null 
							&& sectionService.sectionIsCompleted(ISISession.get().getStudent(), (ISIXmlSection) sec2)) {
						markReviewedLink.setVisible(true);
					} else {
						markReviewedLink.setVisible(false);
					}
					lev3.add(markReviewedLink);
					
					WebMarkupContainer lev4 = new WebMarkupContainer("lev4");
					lev4.setVisible(isCurrent);
					lev3.add(lev4);
					
					BookmarkablePageLink pageLink = new SectionLinkFactory().linkToPage("link", sec2);
					lev4.add(pageLink);
					pageLink.setEnabled(false);
					pageLink.add(new ClassAttributeModifier("current"));
					pageLink.add(new Label("pagenum", "1"));
					
					// Check to see if this page has any messages (read or unread)
					String location = (new ContentLoc(sec2)).getLocation();
					if (locsWithUnread.contains(location)) {
						pageLink.add(new Image("messageIcon", new ResourceReference("img/icons/envelope_new.png")));
					} else if (locsWithMessages.contains(location)) {
						pageLink.add(new Image("messageIcon", new ResourceReference("img/icons/envelope_old.png")));
					} else {
						pageLink.add(new WebMarkupContainer("messageIcon").setVisible(false));
					}
			
					WebMarkupContainer hasResponse = new WebMarkupContainer("hasResponse");
					hasResponse.setVisible(((ISIXmlSection) sec2).hasResponseGroup() && !ISISession.get().getUser().getRole().equals(Role.STUDENT));
					pageLink.add(hasResponse);					
					
				}
			}
		});
	}

	private BookmarkablePageLink<Void> getNextPreviousLink(String name, XmlSection sec, String direction) {
		
		BookmarkablePageLink<Void> link;
		if (sec == null) {
			// No section to link to, Create Disabled Link
			link = new BookmarkablePageLink<Void>(name, ISIStandardPage.class);
			link.add(new Image(direction + "Button", new ResourceReference("img/buttons/" + direction + "_g.png")));
			link.setEnabled(false);
		} else {
		
			sec = new SectionLinkFactory().sectionLinkDest ((ISIXmlSection) sec);
			Class<? extends ISIStandardPage> pageType = ISIApplication.get().getReadingPageClass();
			ContentLoc loc = new ContentLoc (sec);
			link = new BookmarkablePageLink<Void>(name, pageType);
			link.setParameter("loc", loc.getLocation());
			link.add(new Image(direction + "Button", new ResourceReference("img/buttons/" + direction + ".png")));
		}		
		
		return link;
	}

	
}