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

import java.util.List;

import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.cast.cwm.components.ClassAttributeModifier;
import org.cast.cwm.data.Role;
import org.cast.cwm.data.User;
import org.cast.cwm.data.models.UserModel;
import org.cast.cwm.indira.IndiraImage;
import org.cast.cwm.indira.IndiraImageComponent;
import org.cast.cwm.xml.XmlSection;
import org.cast.isi.ISIApplication;
import org.cast.isi.ISISession;
import org.cast.isi.ISIXmlSection;
import org.cast.isi.data.ContentLoc;
import org.cast.isi.data.FeedbackMessage;
import org.cast.isi.data.SectionStatus;
import org.cast.isi.page.ISIStandardPage;
import org.cast.isi.page.SectionLinkFactory;
import org.cast.isi.service.ISIResponseService;
import org.cast.isi.service.ISectionService;
import org.cast.isi.service.SectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class PageLinkPanel extends ISIPanel {

	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(PageLinkPanel.class);

	@Inject
	public ISectionService sectionService;

	private UserModel mUser;
	private IModel<User> mTargetUser;
	private boolean teacher;
	private boolean feedbackRequired = false;

	/**
	 * Construct a set of links to all the pages in a given section.
	 * 
	 * @param id wicket:id of the component
	 * @param mSection the section whose pages to show.  May actually 
	 * be the XmlSection of a page within this section, in which case its section ancestor will be used.
	 * @param mCurrentPage the "current" page to be highlighted in the list; may be null.
	 */
	public PageLinkPanel(String id, IModel<? extends XmlSection> mSection, IModel<? extends XmlSection> mCurrentPage) {
		super(id);

		mUser = new UserModel(ISISession.get().getUser());
		mTargetUser = ISISession.get().getTargetUserModel();
		teacher = mUser.getObject().getRole().equals(Role.TEACHER);

		ISIXmlSection currentPage = mCurrentPage != null ? (ISIXmlSection) mCurrentPage.getObject() : null;
 		ISIXmlSection currentSection = ((ISIXmlSection) mSection.getObject()); // May be itself
		SectionStatus currentSectionStatus = sectionService.getSectionStatus(mTargetUser.getObject(), currentSection);

		// Page Number Repeater
		RepeatingView pageRepeater = new RepeatingView("pageRepeater");
		int pageNumDisplay = 1 + ISIApplication.get().getStudentContent().getLabelIndex(ISIXmlSection.SectionType.PAGE, currentSection.firstPage());

		for(ISIXmlSection page = currentSection.firstPage(); page != null; page = page.getNext(), pageNumDisplay++) {
			WebMarkupContainer pageLinkContainer = new WebMarkupContainer(pageRepeater.newChildId());
			pageRepeater.add(pageLinkContainer);
			
			BookmarkablePageLink<ISIStandardPage> link = new SectionLinkFactory().linkToPage("pageLink", page);
			pageLinkContainer.add(link);
			link.setVisible(ISIApplication.get().isPageNumbersOn());
			link.add(new Label("pageNum", String.valueOf(pageNumDisplay)).setRenderBodyOnly(true));

			feedbackRequired = false;
			EnvelopeImage envelopImage = new EnvelopeImage("icon", new Model<ISIXmlSection>(page));
			
			// for teachers add any of the status icons, for students, just add the envelop
			if (teacher && (currentSectionStatus != null)) {
				// if the page has feedback (red envelop) then display the envelop
				// otherwise, display the needs review icon or no icon at all
				if ((currentSectionStatus.getCompleted() == true) &&
					(currentSectionStatus.getReviewed() == false) && 
					(!feedbackRequired)) {
					IndiraImage ii = IndiraImage.get("img/icons/status_ready.png");
					IndiraImageComponent icon = new IndiraImageComponent("icon", new Model<IndiraImage>(ii));
					link.add(icon);
					icon.add(new SimpleAttributeModifier("alt", "Ready for review"));
					icon.add(new SimpleAttributeModifier("title", "Ready for review"));
					// don't display the status icon if there's no response group on this page
					icon.setVisible(page.hasResponseGroup());
				} else {
					link.add(envelopImage);
				} 
			} else { // just add the envelop for the student
				link.add(envelopImage);
			}
						
			if (page.equals(currentPage)) {
				link.setEnabled(false);
				link.add(new ClassAttributeModifier("current"));
			}
			
			// If the page IS a section, just display it and break out of the loop.
			// Otherwise, the loop will display sibling sections as pages.
			if (page.isSection())
				break;
		}		
		add(pageRepeater);		
	}

	/**
	 * An image of an envelope for a given section if that page has messages.
	 * Set invisible if there are no messages.
	 * 
	 * @author jbrookover
	 *
	 */
	public class EnvelopeImage extends IndiraImageComponent {

		private static final long serialVersionUID = 1L;

		public EnvelopeImage(String id, IModel<ISIXmlSection> model) {
			super(id);

			User student;
			if (ISISession.get().getUser().getRole().equals(Role.STUDENT))
				student = ISISession.get().getUser();
			else
				student = ISISession.get().getStudent();
			List<FeedbackMessage> messages = ISIResponseService.get().getNotesForPage(student, new ContentLoc(model.getObject()).getLocation());
			boolean unread = false;
			for (FeedbackMessage m : messages) {
				if (m.isUnread() 
						&& ((m.getAuthor().getRole().equals(Role.STUDENT) && teacher) ||
							(!m.getAuthor().getRole().equals(Role.STUDENT) && !teacher))) {
					unread = true;
					break;
				}
			}
			if (unread) {
				setDefaultModelObject(IndiraImage.get("img/icons/envelope_new.png"));
				feedbackRequired = true;
			} else if (!messages.isEmpty()) {
				setDefaultModelObject(IndiraImage.get("img/icons/envelope_old.png"));
			} else {
				setVisible(false);
			}
		}
	}	
}