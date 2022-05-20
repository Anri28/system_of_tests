package com.rgr.system_of_tests.service;

import com.rgr.system_of_tests.repo.*;
import com.rgr.system_of_tests.repo.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class TestService {
    @Value("${upload.path}")
    private String uploadPath;
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
    @Autowired
    private MessageSource messageSource;
    public Iterable<Test> getAllTests(){
        Iterable<Test> tests = testsRepository.findAll();
        return tests;
    }
    public Test getTestById(long id){
        Test test = testsRepository.findId(id);
        return test;
    }
    public void EditTest(long id,Map<String, String> form,MultipartFile[] files){
        Test test = testsRepository.findId(id);
        List<Question> q = questionRepository.findByTestId(test.getId());
        questionRepository.deleteAll(q);
        int q_count = 1;
        int a_count = 1;
        Long last_id_q = null;
        int ball = 0;
        for(String key : form.keySet()){
            if(key.equals("isPrivate")){
                if(form.get(key).equals("private")){
                    test.setPrivate(true);
                }
            }if(key.equals("limit")){
                if(form.get(key).equals("Limited")){
                    test.setLimited(true);
                } else {
                    test.setLimited(false);
                }
            }
            if(key.equals("names[]")){
                Long userId = Long.parseLong(form.get(key));
                Invitation inv = new Invitation(test.getId(),userId);
                invitationRepository.save(inv);
                User user = usersRepository.findId(userId);
                String messageForEmail = String.format(
                        messageSource.getMessage("invite",new Object[0],new Locale("ru"))+ "http://localhost:8080/test/%s",
                        user.getFirstname(),
                        test.getId()
                );
                mailSender.send(user.getUsername(),messageSource.getMessage("subject.inv",new Object[0],new Locale("ru")),messageForEmail);
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
                String resultFilename = null;
                try{
                    resultFilename= img(files[q_count-1]);
                }catch (IndexOutOfBoundsException e){
                } catch (Exception e) {
                }
                Question question = new Question(test.getId(),form.get(key));
                question.setFilename(resultFilename);
                if(form.get("img"+q_count)!=null){
                    question.setFilename(form.get("img"+q_count));
                }
                questionRepository.save(question);
                last_id_q =question.getId();
            }
        }
    }
    public void deleteTest(long id){
        Test test = testsRepository.findId(id);
        testsRepository.delete(test);
    }
    public void addTest(Map<String, String> form,MultipartFile[] files){
        Test test = null;
        try {
            test = new Test(form.get("title"), form.get("description"), false);
            testsRepository.save(test);
            int q_count = 1;
            int a_count = 1;
            Long last_id_q = null;
            int ball = 0;
            for (String key : form.keySet()) {
                if (key.equals("isPrivate")) {
                    if (form.get(key).equals("private")) {
                        test.setPrivate(true);
                    }
                }
                if (key.equals("limit")) {
                    if (form.get(key).equals("Limited")) {
                        test.setLimited(true);
                    } else {
                        test.setLimited(false);
                    }
                }
                if (key.equals("names[]")) {
                    Long userId = Long.parseLong(form.get(key));
                    Invitation inv = new Invitation(test.getId(), userId);
                    invitationRepository.save(inv);
                    User user = usersRepository.findId(userId);
                    String messageForEmail = String.format(
                            messageSource.getMessage("invite",new Object[0],new Locale("ru"))+"http://localhost:8080/test/%s",
                            user.getFirstname(),
                            test.getId()
                    );
                    mailSender.send(user.getUsername(), messageSource.getMessage("subject.inv",new Object[0],new Locale("ru")), messageForEmail);
                }
                if (key.equals("a" + q_count + a_count)) {
                    ball = Integer.parseInt(form.get("b" + q_count + a_count));
                    Answer answer = new Answer(last_id_q, form.get(key), ball);
                    answerRepository.save(answer);
                    a_count++;
                    if (a_count == 3) {
                        if (!form.containsKey("a" + q_count + a_count)) {
                            q_count++;
                            a_count = 1;
                        }
                    }
                    if (a_count == 4) {
                        a_count = 1;
                        q_count++;
                    }
                }
                if (key.equals("q" + q_count)) {
                    String resultFilename = null;
                    try {
                        resultFilename = img(files[q_count - 1]);
                    } catch (IndexOutOfBoundsException e) {
                    }
                    Question question = new Question(test.getId(), form.get(key));
                    question.setFilename(resultFilename);
                    questionRepository.save(question);
                    last_id_q = question.getId();
                }
            }
        } catch (Exception e) {
            System.out.println("exc");
            testsRepository.delete(test);
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
                        answers.get(0).getId(),answers.get(1).getId(),answers.get(2).getId(),q.getId(),q.getFilename(),answers.get(0).getScore(),answers.get(1).getScore(),answers.get(2).getScore());
                qm.add(questionModel);
            }catch (IndexOutOfBoundsException e){
                QuestionModel questionModel = new QuestionModel(q.getQuestion_text(),answers.get(0).getAnswer(),answers.get(1).getAnswer(),null,
                        answers.get(0).getId(),answers.get(1).getId(),null,q.getId(),q.getFilename(),answers.get(0).getScore(),answers.get(1).getScore(),0);
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
                messageSource.getMessage("testResult.mess",new Object[0],new Locale("ru")),
                name,
                result,
                max
        );
        String messageForEmail = String.format(
                messageSource.getMessage("testResult.mess.forMail",new Object[0],new Locale("ru")),
                test.getTitle(),
                name,
                result,
                max

        );
        mailSender.send(email,"Результат тестирования",messageForEmail);
        return message;

    }

    public String img(MultipartFile file) throws Exception {
        File uploadDir = new File(uploadPath);
        if(!uploadDir.exists()){
            uploadDir.mkdir();
        }
        if(file.getSize()!=0){
            if(file.getOriginalFilename().endsWith(".jpg")!=true){
                System.out.println("suffix");
                throw new Exception("suffix is not .jpg");
            }
            String uuidFile = UUID.randomUUID().toString();
            String resultFilename = uuidFile + "." + file.getOriginalFilename();
            file.transferTo(new File(uploadPath + "/" + resultFilename));
            return resultFilename;
        }
        return null;
    }
}
