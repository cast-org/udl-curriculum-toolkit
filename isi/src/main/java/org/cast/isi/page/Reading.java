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
package org.cast.isi.page;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.cast.cwm.components.ClassAttributeModifier;
import org.cast.cwm.data.Prompt;
import org.cast.cwm.data.ResponseMetadata;
import org.cast.cwm.data.Role;
import org.cast.cwm.data.User;
import org.cast.cwm.data.component.highlight.HighlightDisplayPanel;
import org.cast.cwm.data.models.UserModel;
import org.cast.cwm.tag.component.TagPanel;
import org.cast.cwm.xml.XmlSectionModel;
import org.cast.isi.CollapseBoxBehavior;
import org.cast.isi.ISIApplication;
import org.cast.isi.ISISession;
import org.cast.isi.ISIXmlComponent;
import org.cast.isi.ISIXmlSection;
import org.cast.isi.data.ContentElement;
import org.cast.isi.data.ContentLoc;
import org.cast.isi.data.PromptType;
import org.cast.isi.panel.HighlightControlPanel;
import org.cast.isi.panel.MiniGlossaryModal;
import org.cast.isi.panel.PageNavPanel;
import org.cast.isi.panel.ResponseButtons;
import org.cast.isi.panel.ResponseFeedbackPanel;
import org.cast.isi.panel.ResponseList;
import org.cast.isi.panel.StudentSectionCompleteTogglePanel;
import org.cast.isi.service.IFeatureService;
import org.cast.isi.service.IISIResponseService;
import org.cast.isi.service.IQuestionService;
import org.cast.isi.validator.QuestionNameValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@AuthorizeInstantiation("STUDENT")
public class Reading extends ISIStandardPage implements IHeaderContributor {
	
	protected final boolean showXmlContent;

	protected static final Logger log = LoggerFactory.getLogger(Reading.class);

	protected QuestionListView questionList;
	protected WebMarkupContainer questionContainer;
	protected IModel<User> mTargetUser;
	protected XmlSectionModel mSection;
	protected IModel<Prompt> mNotesPrompt;
	protected ResponseMetadata pageNotesMetadata = new ResponseMetadata();
	protected boolean showSectionToggleTextLink;

	@Inject
	protected IQuestionService questionService;

	@Inject
	protected IISIResponseService responseService;

	@Inject
	protected IFeatureService featureService;

	public Reading (PageParameters parameters) {
		this(parameters, true);
	}
	
	/**
	 * Construct page, allowing for a reading page that doesn't show the 
	 * actual main page content (the XML)... this happens for instance when
	 * a teacher has not selected a student to work with. 
	 * @param parameters
	 * @param showXmlContent
	 */
	public Reading (PageParameters parameters, boolean showXmlContent) {
		super(parameters);
		this.showXmlContent = showXmlContent;
		showSectionToggleTextLink = featureService.isSectionToggleTextLinksOn();
		pageTitle = (new StringResourceModel("Reading.pageTitle", this, null, "Reading").getString());
		setPageTitle(pageTitle);

		mTargetUser = ISISession.get().getTargetUserModel();
		boolean teacher = ISISession.get().getUser().getRole().equals(Role.TEACHER);

    	setLoc(parameters);
    	add(ISIApplication.get().getNavBar("navbar", mSection, teacher));    	
		addXmlComponent((ISIXmlSection) mSection.getObject());
		addSectionCompleteToggle((ISIXmlSection) mSection.getObject());
    	addNotesPanel();		
		addHighlightPanel();
		addTaggingPanel();
		addQuestionsPanel();
		
		PageNavPanel bottomNavPanel = new PageNavPanel("pageNavPanelBottom", mSection);
		add(bottomNavPanel);
	}

	@Override
	public boolean hasMiniGlossary() {
		return true;
	}
	
	protected void setLoc(PageParameters parameters) {
		// setup the loc of this reading page, check the parameters, then
		// the bookmark and then finally the first page		
		if (parameters.containsKey("loc")) {
			loc = new ContentLoc(parameters.getString("loc"));
		} else if (parameters.containsKey("pageNum")) {
			loc = new ContentLoc(ISIApplication.get().getPageNum(parameters.getInt(("pageNum")) - 1));
		} else {
			loc = new ContentLoc(ISIApplication.get().getBookmarkLoc().getLocation());
		}
		if (loc == null || loc.getSection() == null) {
			loc = new ContentLoc(ISIApplication.get().getPageNum(0));
		}
				
    	ISIXmlSection section = loc.getSection();
    	if (section != null) {
    		ISISession.get().setBookmark(loc);    		
    	}
    	mSection = new XmlSectionModel(section);
	}
	
	
	protected void addSectionCompleteToggle(ISIXmlSection section) {
		add(new StudentSectionCompleteTogglePanel("toggleCompletePanel", mSection, mTargetUser));
	}

	protected void addXmlComponent (ISIXmlSection section) {
		MiniGlossaryModal miniGlossaryModal = new MiniGlossaryModal("miniGlossaryModal", getPageName());
		add(miniGlossaryModal);

		ResponseFeedbackPanel responseFeedbackPanel = new ResponseFeedbackPanel("responseFeedbackPanel");
		add(responseFeedbackPanel);
		
		if (showXmlContent) {
			ISIXmlComponent xmlComponent = makeXmlComponent("xmlContent",  mSection);
			xmlComponent.setContentPage(getPageName());
			xmlComponent.setMiniGlossaryModal(miniGlossaryModal);
			xmlComponent.setResponseFeedbackPanel(responseFeedbackPanel);
			add(xmlComponent);
		} else {
			add (new WebMarkupContainer("xmlContent").setVisible(false));
		}
	}
	
	protected ISIXmlComponent makeXmlComponent (String wicketId, XmlSectionModel model) {
		ISIXmlComponent component = new ISIXmlComponent(wicketId, model, "student");
		component.setTransformParameter("lock-response", isLockResponse(model));
		component.setTransformParameter("delay-feedback", isDelayFeedback(model));
		if (userIsStudent())
			component.setTransformParameter("strip-class", "teacheronly");
		return component;
	}

	protected boolean userIsStudent() {
		return true;
	}

	private boolean isDelayFeedback(XmlSectionModel model) {
		ISIXmlSection section = (ISIXmlSection) model.getObject();
		return (section != null) && section.isDelayFeedback();
	}

	private boolean isLockResponse(XmlSectionModel model) {
		ISIXmlSection section = (ISIXmlSection) model.getObject();
		return (section != null) && section.isLockResponse();
	}

	protected void addNotesPanel () {
		setPageNotesMetadata();
		mNotesPrompt = responseService.getOrCreatePrompt(PromptType.PAGE_NOTES, loc);
		WebMarkupContainer notesbox = new WebMarkupContainer("notesbox") {
			private static final long serialVersionUID = 1L;
			@Override
			protected void onBeforeRender() {
				super.onBeforeRender();
				if (mNotesPrompt != null || !(mNotesPrompt.getObject().getResponses().size() == 0))
					this.add(new ClassAttributeModifier("open"));
			}
		};
		add(notesbox);
		notesbox.add(new WebMarkupContainer("collapseBoxToggle").add(new CollapseBoxBehavior("onclick", "pagenotes", getPageName())));
		ResponseList responseList = new ResponseList ("responseList", mNotesPrompt, pageNotesMetadata, loc);
		responseList.setContext("pagenote");
		responseList.setAllowNotebook(ISIApplication.get().isNotebookOn());
		responseList.setAllowWhiteboard(ISIApplication.get().isWhiteboardOn());
		notesbox.add(responseList);
		ResponseButtons responseButtons = new ResponseButtons("responseButtons", mNotesPrompt, pageNotesMetadata, loc);
		responseButtons.setContext("pagenote");
		notesbox.add(responseButtons);
		notesbox.setVisible(ISIApplication.get().isPageNotesOn());
	}

	
	protected void setPageNotesMetadata() {
		pageNotesMetadata.addType("TEXT");
		pageNotesMetadata.addType("AUDIO");
	}

	public void addHighlightPanel() {	
		add(new HighlightControlPanel("highlightControlPanel", loc, mSection).setVisible(ISIApplication.get().isHighlightsPanelOn()));
		add(new HighlightDisplayPanel("highlightDisplayPanel", responseService.getOrCreatePrompt(PromptType.PAGEHIGHLIGHT, loc)).setVisible(ISIApplication.get().isHighlightsPanelOn()));
	}
	
	protected void addTaggingPanel () {
		ContentElement ce = responseService.getOrCreateContentElement(loc).getObject();
		WebMarkupContainer tagsBox = new WebMarkupContainer("tagsBox");
		add(tagsBox);
		tagsBox.setVisible(ISIApplication.get().isTagsOn());
		tagsBox.add(new WebMarkupContainer("tagCollapseToggle").add(new CollapseBoxBehavior("onclick", "tagpanel:reading", getPageName())));
		tagsBox.add(new TagPanel("tagPanel", ce, ISIApplication.get().getTagLinkBuilder()));
	}
	
	@SuppressWarnings("static-access")
	protected void addQuestionsPanel () {
    	// Always open this panel if there are questions
		WebMarkupContainer questionBox = new WebMarkupContainer("questionBox") {
			private static final long serialVersionUID = 1L;
			@Override
			protected void onBeforeRender() {
				super.onBeforeRender();
				if (questionList.size() > 0) {
					this.add(new ClassAttributeModifier("open"));
				}
			}
		};
		add(questionBox);
		questionBox.setVisible(ISIApplication.get().isMyQuestionsOn());
		questionBox.add(new WebMarkupContainer("questionBoxToggle")
					.add(new CollapseBoxBehavior("onclick", "questionspanel", getPageName())));
    	questionContainer = new WebMarkupContainer("questionContainer");
    	questionBox.add(questionContainer);
		questionContainer.setOutputMarkupId(true);
		PopupSettings questionPopupSettings = ISIApplication.get().questionPopupSettings;
    	questionList = new QuestionListView("question", QuestionPopup.class, questionPopupSettings, null, null);
		questionContainer.add(questionList);
		questionContainer.add(new WebMarkupContainer("qButtonVisible"));
    	add(new NewQuestionForm("newQuestion"));
	}
	
	protected class NewQuestionForm extends Form<Object> {
		private static final long serialVersionUID = 1L;
		Model<String> textModel = new Model<String>("");
		private FeedbackPanel feedback;

		public NewQuestionForm(String id) {
			super(id);
			add(new TextArea<String>("text", textModel)
					.add(new QuestionNameValidator(null))
					.setRequired(true)
					.add(new SimpleAttributeModifier("maxlength", "250")));
			add(new AjaxButton("submit") {
				private static final long serialVersionUID = 1L;

				@Override
				protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
					super.onSubmit();
					String qstr = ((String) textModel.getObject());
					textModel.setObject("");
					if (qstr != null)
						qstr = qstr.trim();
					if (!Strings.isEmpty(qstr)) {
						if (qstr.length() > 250)
							qstr = qstr.substring(0, 250);
						questionService.createQuestion(new UserModel(mTargetUser.getObject()), qstr, getPageName());
						questionList.doQuery();
						target.addComponent(questionContainer);
						target.addComponent(NewQuestionForm.this);
					}
					target.appendJavascript("$('#newQuestionModal').hide();");
				}
				@Override
				protected void onError(AjaxRequestTarget target, Form<?> form) {
					super.onError(target, form);
					if (target != null)
						target.addComponent(feedback);
				}	
			});
			add(feedback = new FeedbackPanel("feedback", new ContainerFeedbackMessageFilter(NewQuestionForm.this)));
			feedback.setOutputMarkupPlaceholderTag(true);
		}
	}

	
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
	}
	
	@Override
	public String getPageType() {
		return "reading";
	}
	
	@Override
	public String getPageName() {
		return loc.getLocation();
	}
	@Override
	protected void onDetach() {
		if (mTargetUser != null)
			mTargetUser.detach();
		if (mNotesPrompt != null)
			mNotesPrompt.detach();
		super.onDetach();
	}
	
}