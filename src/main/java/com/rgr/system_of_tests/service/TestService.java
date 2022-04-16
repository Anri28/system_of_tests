package com.rgr.system_of_tests.service;

import com.rgr.system_of_tests.repo.*;
import com.rgr.system_of_tests.repo.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class TestService {
    @Autowired
    private MailSender mailSender;
    @Autowired
    private UserService usersService;
    @Autowired
    private InvitationRepository invitationRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private TestsRepository testsRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AnswerRepository answerRepository;
    public Iterable<Test> getAllTests(){
        Iterable<Test> tests = testsRepository.findAll();
        return tests;
    }
    public Test getTestById(long id){
        Test test = testsRepository.findId(id);
        return test;
    }
    public void EditTest(long id,String title,String description){
        Test test = testsRepository.findId(id);
        test.setTitle(title);
        test.setDescription(description);
        testsRepository.save(test);
    }
    public void deleteTest(long id){
        Test test = testsRepository.findId(id);
        testsRepository.delete(test);
    }
    public void addTest(Map<String, String> form,String title, String description){
        Test test = new Test(title,description,false);
        testsRepository.save(test);
        int q_count = 1;
        int a_count = 1;
        Long last_id_q = null;
        int ball = 0;
        for(String key : form.keySet()){
            if(key.equals("isPrivate")){
                if(form.get(key).equals("private")){
                    test.setPrivate(true);
                }
            }
            if(key.equals("a"+q_count+a_count)){
                ball = Integer.parseInt(form.get("b"+q_count+a_count));
                Answer answer = new Answer(last_id_q,form.get(key),ball);
                answerRepository.save(answer);
                a_count++;
                if(a_count==3){
                    if(!form.containsKey("a"+q_count+a_count)){q_count++; a_count=1;}
                }
                if(a_count==4){a_count=1;q_count++;}
            }
            if(key.equals("q"+q_count)){
                Question question = new Question(test.getId(),form.get(key));
                questionRepository.save(question);
                last_id_q =question.getId();
            }
        }
    }
    public ArrayList<Test> testsSearch(String date,String search){
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date1 = null;
        try {
            date1 = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        ArrayList<Test> resTests = testsRepository.findByDate(date1);
        ArrayList<Test> TestsForModel = new ArrayList<>();
        for(Test t:resTests){
            if(t.getTitle().toLowerCase().contains(search.toLowerCase()) || t.getDescription().toLowerCase().contains(search.toLowerCase())){
                TestsForModel.add(t);
            }
        }
        return TestsForModel;
    }
    public ArrayList<QuestionModel> testViewById(long id){
        Test test = testsRepository.findId(id);
        User user = usersRepository.findByUsername(usersService.getCurrentUsername());
        if(user.ifRole("ADMIN")==false&&user.ifRole("TESTER")==false){
            if(test.getPrivate()){
                Invitation invitation = invitationRepository.findId(test.getId(),user.getId());
                if(invitation==null){
                    return null;
                }
            }
        }
        List<Question> questions = questionRepository.findByTestId(id);
        ArrayList<QuestionModel> qm = new ArrayList<>();
        for(Question q : questions){
            List<Answer>  answers = answerRepository.findByQuestionId(q.getId());
            try{
                QuestionModel questionModel = new QuestionModel(q.getQuestion_text(),answers.get(0).getAnswer(),answers.get(1).getAnswer(),answers.get(2).getAnswer(),
                        answers.get(0).getId(),answers.get(1).getId(),answers.get(2).getId(),q.getId());
                qm.add(questionModel);
            }catch (IndexOutOfBoundsException e){
                QuestionModel questionModel = new QuestionModel(q.getQuestion_text(),answers.get(0).getAnswer(),answers.get(1).getAnswer(),null,
                        answers.get(0).getId(),answers.get(1).getId(),null,q.getId());
                qm.add(questionModel);
            }
        }
        return qm;
    }
    public String getTestResult(long id,Map<String, String> form){

        int result=0;
        Test test = testsRepository.findId(id);
        int max=0;
        String name = "";
        String email = "";
        List<Answer> answers = answerRepository.findAllByTest(id);
        for(Answer a : answers){
            max+=a.getScore();
        }
        for(String key: form.keySet()){
            if(key.equals("userId")){
                email = form.get(key);
                User user = usersRepository.findByName(email);
                name = user.getFirstname();
                continue;}
            Long b = Long.parseLong(form.get(key));
            Answer answer = answerRepository.findBy_Id(b);
            result+=answer.getScore();
        }
        String message = String.format(
                "%s, вы набрали %s баллов из %s возможных",
                name,
                result,
                max
        );
        String messageForEmail = String.format(
                "%s.\n"+
                        "%s, вы набрали %s баллов из %s возможных",
                test.getTitle(),
                name,
                result,
                max

        );
        mailSender.send(email,"Результат тестированя",messageForEmail);
        return message;

    }
}