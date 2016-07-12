package teammates.storage.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.Query;
import javax.jdo.Transaction;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import teammates.common.datatransfer.EntityAttributes;
import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.FeedbackQuestionAttributes;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.storage.entity.Question;
import teammates.storage.entity.FeedbackSession;

public class FeedbackQuestionsDb extends EntitiesDb {
    public static final String ERROR_UPDATE_NON_EXISTENT = "Trying to update non-existent Feedback Question : ";
    
    @Override
    public List<EntityAttributes> createEntities(Collection<? extends EntityAttributes> entitiesToAdd) {
        Assumption.fail(
                "Use createFeedbackQuestions(FeedbackSessionAttributes, Collection<FeedbackQuestionAttributes>)");
        return null;
    }
    
    @Override
    public Object createEntity(EntityAttributes entityToAdd)
            throws InvalidParametersException, EntityAlreadyExistsException {
        Assumption.fail(
                "Use createFeedbackQuestions(FeedbackSessionAttributes, FeedbackQuestionAttributes)");
        return null;
    }
    
    public void createFeedbackQuestions(FeedbackSessionAttributes session,
                Collection<FeedbackQuestionAttributes> questionsToAdd)
            throws InvalidParametersException, EntityDoesNotExistException {
        
        Transaction txn = getPm().currentTransaction();
        try {
            txn.begin();
            FeedbackSession fs = new FeedbackSessionsDb().getEntity(session);
            
            if (fs == null) {
                throw new EntityDoesNotExistException(
                        ERROR_UPDATE_NON_EXISTENT + session.toString());
            }
            
            for (FeedbackQuestionAttributes questionToAdd : questionsToAdd) {
                questionToAdd.sanitizeForSaving();
                
                if (!questionToAdd.isValid()) {
                    throw new InvalidParametersException(questionToAdd.getInvalidityInfo());
                }
                
                fs.getFeedbackQuestions().add(questionToAdd.toEntity());
                
                log.info(questionToAdd.getBackupIdentifier());
            }
            
            getPm().currentTransaction().commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
            getPm().close();
        }
    }
    
    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return Null if not found.
     */
    public FeedbackQuestionAttributes getFeedbackQuestion(FeedbackSessionAttributes fsa, 
                                                          String feedbackQuestionId) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, feedbackQuestionId);

        Question fq = getFeedbackQuestionEntity(fsa, feedbackQuestionId);
        
        if (fq == null) {
            log.info("Trying to get non-existent Question: " + feedbackQuestionId);
            return null;
        }
        
        return new FeedbackQuestionAttributes(fq);
    }

    public void createFeedbackQuestion(FeedbackSessionAttributes fsa, FeedbackQuestionAttributes question) 
            throws InvalidParametersException, EntityDoesNotExistException, EntityAlreadyExistsException {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, fsa);
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, question);
        
        new FeedbackSessionsDb().addQuestionToSession(fsa, question);
    }
    
    public void createFeedbackQuestionWithoutExistenceCheck(FeedbackSessionAttributes fsa, FeedbackQuestionAttributes question) 
            throws InvalidParametersException, EntityDoesNotExistException {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, fsa);
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, question);
        
        new FeedbackSessionsDb().addQuestionToSessionWithoutExistenceCheck(fsa, question);
    }
    
    public void createFeedbackQuestionWithoutFlushing(FeedbackSessionAttributes fsa, FeedbackQuestionAttributes question) 
            throws InvalidParametersException, EntityDoesNotExistException, EntityAlreadyExistsException {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, fsa);
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, question);
        
        new FeedbackSessionsDb().addQuestionToSessionWithoutFlushing(fsa, question);
    }
    
    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return Null if not found.
     */
    public FeedbackQuestionAttributes getFeedbackQuestion(
            String feedbackSessionName,
            String courseId,
            int questionNumber) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, feedbackSessionName);
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, courseId);
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, questionNumber);

        Question fq = getFeedbackQuestionEntity(feedbackSessionName,
                courseId, questionNumber);
        
        if (fq == null) {
            log.info("Trying to get non-existent Question: "
                     + questionNumber + "." + feedbackSessionName + "/" + courseId);
            return null;
        }
        
        return new FeedbackQuestionAttributes(fq);
    }
    
    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return An empty list if no such questions are found.
     */
    public List<FeedbackQuestionAttributes> getFeedbackQuestionsForSession(
            String feedbackSessionName, String courseId) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, feedbackSessionName);
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, courseId);

        List<Question> questions = getFeedbackQuestionEntitiesForSession(feedbackSessionName, courseId);
        return getFeedbackQuestionAttributesFromFeedbackQuestions(questions);
    }
    
    public List<FeedbackQuestionAttributes> getFeedbackQuestionsForSession(FeedbackSession feedbackSession) {
        return getFeedbackQuestionAttributesFromFeedbackQuestions(feedbackSession.getFeedbackQuestions());
    }

    public static List<FeedbackQuestionAttributes> getFeedbackQuestionAttributesFromFeedbackQuestions(
                                                        Collection<Question> questions) {
        List<FeedbackQuestionAttributes> fqList = new ArrayList<FeedbackQuestionAttributes>();

        for (Question question : questions) {
            if (!JDOHelper.isDeleted(question)) {
                fqList.add(new FeedbackQuestionAttributes(question));
            }
        }
        
        Collections.sort(fqList);
        return fqList;
    }
    
    public static List<Question> getFeedbackQuestionEntitiesFromFeedbackQuestionAttributes(
            Collection<FeedbackQuestionAttributes> questions) {
        
        if (questions == null) {
            return new ArrayList<Question>();
        }
        
        List<Question> fqList = new ArrayList<Question>();
        for (FeedbackQuestionAttributes question : questions) {
            fqList.add(question.toEntity());
        }
        return fqList;
    }
    
    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return An empty list if no such questions are found.
     */
    public List<FeedbackQuestionAttributes> getFeedbackQuestionsForGiverType(
            String feedbackSessionName, String courseId, FeedbackParticipantType giverType) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, feedbackSessionName);
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, courseId);
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, giverType);

        List<Question> questions = getFeedbackQuestionEntitiesForGiverType(
                feedbackSessionName, courseId, giverType);
        return getFeedbackQuestionAttributesFromFeedbackQuestions(questions);
    }
    
    /**
     * Preconditions: <br>
     * * All parameters are non-null.
     * @return An empty list if no such questions are found.
     */
    public List<FeedbackQuestionAttributes> getFeedbackQuestionsForCourse(String courseId) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, courseId);

        List<Question> questions = getFeedbackQuestionEntitiesForCourse(courseId);
        return getFeedbackQuestionAttributesFromFeedbackQuestions(questions);
    }
    
    /**
     * Updates the feedback question identified by `{@code newAttributes.getId()}
     *   and changes the {@code updatedAt} timestamp to be the time of update.
     * For the remaining parameters, the existing value is preserved
     *   if the parameter is null (due to 'keep existing' policy).<br>
     * 
     * Preconditions: <br>
     * * {@code newAttributes.getId()} is non-null and
     *  correspond to an existing feedback question. <br>
     */
    public void updateFeedbackQuestion(FeedbackQuestionAttributes newAttributes)
            throws InvalidParametersException, EntityDoesNotExistException {
        updateFeedbackQuestion(newAttributes, false);
    }
    
    /**
     * Updates the feedback question identified by `{@code newAttributes.getId()}
     * For the remaining parameters, the existing value is preserved
     *   if the parameter is null (due to 'keep existing' policy).<br>
     * The timestamp for {@code updatedAt} is independent of the {@code newAttributes}
     *   and depends on the value of {@code keepUpdateTimestamp}
     * Preconditions: <br>
     * * {@code newAttributes.getId()} is non-null and
     *  correspond to an existing feedback question. <br>
     */
    public void updateFeedbackQuestion(FeedbackQuestionAttributes newAttributes, boolean keepUpdateTimestamp)
            throws InvalidParametersException, EntityDoesNotExistException {
        
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, newAttributes);
        
        // TODO: Sanitize values and update tests accordingly
        
        updateFeedbackQuestionWithoutFlushing(newAttributes, keepUpdateTimestamp);
        
        log.info(newAttributes.getBackupIdentifier());
        getPm().close();
    }
    
    public void updateFeedbackQuestionWithoutFlushing(FeedbackQuestionAttributes newAttributes) throws InvalidParametersException, EntityDoesNotExistException {
        updateFeedbackQuestionWithoutFlushing(newAttributes, false);
    }

    public void updateFeedbackQuestionWithoutFlushing(FeedbackQuestionAttributes newAttributes,
            boolean keepUpdateTimestamp) throws InvalidParametersException, EntityDoesNotExistException {
        if (!newAttributes.isValid()) {
            throw new InvalidParametersException(newAttributes.getInvalidityInfo());
        }
        
        Question fq = (Question) getEntity(newAttributes);
        
        if (fq == null) {
            throw new EntityDoesNotExistException(
                    ERROR_UPDATE_NON_EXISTENT + newAttributes.toString());
        }
        
        fq.setQuestionNumber(newAttributes.questionNumber);
        fq.setQuestionText(newAttributes.questionMetaData);
        fq.setQuestionType(newAttributes.questionType);
        fq.setGiverType(newAttributes.giverType);
        fq.setRecipientType(newAttributes.recipientType);
        fq.setShowResponsesTo(newAttributes.showResponsesTo);
        fq.setShowGiverNameTo(newAttributes.showGiverNameTo);
        fq.setShowRecipientNameTo(newAttributes.showRecipientNameTo);
        fq.setNumberOfEntitiesToGiveFeedbackTo(newAttributes.numberOfEntitiesToGiveFeedbackTo);
        
        //set true to prevent changes to last update timestamp
        fq.keepUpdateTimestamp = keepUpdateTimestamp;
    }
    
    public void deleteQuestion(FeedbackSessionAttributes fsa, FeedbackQuestionAttributes questionToDelete) 
            throws EntityDoesNotExistException {
        Transaction txn = getPm().currentTransaction();
        try {
            txn.begin();
            FeedbackSession fs = new FeedbackSessionsDb().getEntity(fsa);
            
            if (fs == null) {
                throw new EntityDoesNotExistException(
                        ERROR_UPDATE_NON_EXISTENT + fsa.toString());
            }
            
            fs.getFeedbackQuestions().remove(questionToDelete);
            deleteEntity(questionToDelete);
            
            getPm().currentTransaction().commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
            getPm().close();
        }
    }
    
    public void deleteFeedbackQuestionsForCourse(String courseId) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, courseId);
        
        List<String> courseIds = new ArrayList<String>();
        courseIds.add(courseId);
        deleteFeedbackQuestionsForCourses(courseIds);
    }
    

    public void deleteFeedbackQuestionsForCourses(List<String> courseIds) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, courseIds);
        
        List<Question> feedbackQuestionList = getFeedbackQuestionEntitiesForCourses(courseIds);
        
        getPm().deletePersistentAll(feedbackQuestionList);
        getPm().flush();
    }
    
    private List<Question> getFeedbackQuestionEntitiesForCourses(List<String> courseIds) {
        Query q = getPm().newQuery(Question.class);
        q.setFilter(":p.contains(courseId)");
        
        @SuppressWarnings("unchecked")
        List<Question> feedbackQuestionList = (List<Question>) q.execute(courseIds);
        
        return feedbackQuestionList;
    }
    
    private Question getFeedbackQuestionEntity(FeedbackSessionAttributes fsa, String feedbackQuestionId) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, feedbackQuestionId);

        Key k = KeyFactory.createKey(FeedbackSession.class.getSimpleName(), fsa.getId())
                            .getChild(Question.class.getSimpleName(), feedbackQuestionId);
        try {
            return getPm().getObjectById(Question.class, k);
        } catch (JDOObjectNotFoundException e) {
            return null;
        }
    }
    
    // Gets a feedbackQuestion based on feedbackSessionName and questionNumber.
    private Question getFeedbackQuestionEntity(
            String feedbackSessionName, String courseId, int questionNumber) {
        
        Query q = getPm().newQuery(Question.class);
        q.declareParameters("String feedbackSessionNameParam, String courseIdParam, int questionNumberParam");
        q.setFilter("feedbackSessionName == feedbackSessionNameParam && "
                    + "courseId == courseIdParam && "
                    + "questionNumber == questionNumberParam");
        
        @SuppressWarnings("unchecked")
        List<Question> feedbackQuestionList =
                (List<Question>) q.execute(feedbackSessionName, courseId, questionNumber);
        
        if (feedbackQuestionList.size() > 1) {
            log.severe("More than one question with same question number in " 
                      + courseId + "/" + feedbackSessionName + " question " + questionNumber);
        }
        
        if (feedbackQuestionList.isEmpty() || JDOHelper.isDeleted(feedbackQuestionList.get(0))) {
            return null;
        }
        
        return feedbackQuestionList.get(0);
    }
    
    private List<Question> getFeedbackQuestionEntitiesForSession(
            String feedbackSessionName, String courseId) {
        Query q = getPm().newQuery(Question.class);
        q.declareParameters("String feedbackSessionNameParam, String courseIdParam");
        q.setFilter("feedbackSessionName == feedbackSessionNameParam && courseId == courseIdParam");
        
        @SuppressWarnings("unchecked")
        List<Question> feedbackQuestionList =
                (List<Question>) q.execute(feedbackSessionName, courseId);
        
        return feedbackQuestionList;
    }
    
    private List<Question> getFeedbackQuestionEntitiesForCourse(String courseId) {
        Query q = getPm().newQuery(Question.class);
        q.declareParameters("String courseIdParam");
        q.setFilter("courseId == courseIdParam");
        
        @SuppressWarnings("unchecked")
        List<Question> feedbackQuestionList = (List<Question>) q.execute(courseId);
        
        return feedbackQuestionList;
    }
    
    private List<Question> getFeedbackQuestionEntitiesForGiverType(
            String feedbackSessionName, String courseId, FeedbackParticipantType giverType) {
        Query q = getPm().newQuery(Question.class);
        q.declareParameters("String feedbackSessionNameParam, "
                            + "String courseIdParam, "
                            + "FeedbackParticipantType giverTypeParam");
        q.declareImports("import teammates.common.datatransfer.FeedbackParticipantType");
        q.setFilter("feedbackSessionName == feedbackSessionNameParam && "
                    + "courseId == courseIdParam && "
                    + "giverType == giverTypeParam ");
        
        @SuppressWarnings("unchecked")
        List<Question> feedbackQuestionList =
                (List<Question>) q.execute(feedbackSessionName, courseId, giverType);
        
        return feedbackQuestionList;
    }
    
    @Override
    protected Object getEntity(EntityAttributes attributes) {
        FeedbackQuestionAttributes feedbackQuestionToGet = (FeedbackQuestionAttributes) attributes;
        
        if (feedbackQuestionToGet.getId() != null) {
            FeedbackSessionAttributes fs = new FeedbackSessionAttributes();
            fs.setCourseId(feedbackQuestionToGet.courseId);
            fs.setFeedbackSessionName(feedbackQuestionToGet.feedbackSessionName);
            return getFeedbackQuestionEntity(fs, feedbackQuestionToGet.getId());
        }
        
        return getFeedbackQuestionEntity(
                feedbackQuestionToGet.feedbackSessionName,
                feedbackQuestionToGet.courseId,
                feedbackQuestionToGet.questionNumber);
    }

    public void saveQuestionAndAdjustQuestionNumbers(FeedbackSessionAttributes session,
                FeedbackQuestionAttributes questionToAddOrUpdate,
                boolean isUpdating,
                int oldQuestionNumber)
            throws InvalidParametersException, EntityDoesNotExistException, EntityAlreadyExistsException {
        Transaction txn = getPm().currentTransaction();
        try {
            txn.begin();
            
            FeedbackSession fs = new FeedbackSessionsDb().getEntity(session);
            
            if (fs == null) {
                throw new EntityDoesNotExistException("Session disappeared");
            }
            
            List<FeedbackQuestionAttributes> questions = getFeedbackQuestionsForSession(fs);
            if (oldQuestionNumber <= 0) {
                oldQuestionNumber = questions.size() + 1;
            }
            if (questionToAddOrUpdate.questionNumber <= 0) {
                questionToAddOrUpdate.questionNumber = questions.size() + 1;
            }
            adjustQuestionNumbersWithoutCommitting(oldQuestionNumber, questionToAddOrUpdate.questionNumber , questions);
            if (isUpdating) {
                updateFeedbackQuestionWithoutFlushing(questionToAddOrUpdate);
            } else {
                questionToAddOrUpdate.setId(questionToAddOrUpdate.makeId());
                createFeedbackQuestionWithoutFlushing(session, questionToAddOrUpdate);
            }
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
            getPm().close();
        }
    }

    private void adjustQuestionNumbersWithoutCommitting(int oldQuestionNumber, int newQuestionNumber,
            List<FeedbackQuestionAttributes> questions) {
        if (oldQuestionNumber > newQuestionNumber && oldQuestionNumber >= 1) {
            for (int i = oldQuestionNumber - 1; i >= newQuestionNumber; i--) {
                FeedbackQuestionAttributes question = questions.get(i - 1);
                question.questionNumber += 1;
                try {
                    updateFeedbackQuestionWithoutFlushing(question, false);
                } catch (InvalidParametersException e) {
                    Assumption.fail("Invalid question. " + e);
                } catch (EntityDoesNotExistException e) {
                    Assumption.fail("Question disappeared." + e);
                }
            }
        } else if (oldQuestionNumber < newQuestionNumber && oldQuestionNumber < questions.size()) {
            for (int i = oldQuestionNumber + 1; i <= newQuestionNumber; i++) {
                FeedbackQuestionAttributes question = questions.get(i - 1);
                question.questionNumber -= 1;
                try {
                    updateFeedbackQuestionWithoutFlushing(question, false);
                } catch (InvalidParametersException e) {
                    Assumption.fail("Invalid question." + e);
                } catch (EntityDoesNotExistException e) {
                    Assumption.fail("Question disappeared." + e);
                }
            }
        }
    }
}
