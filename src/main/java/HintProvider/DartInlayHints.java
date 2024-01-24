package HintProvider;

import com.google.dart.server.GetHoverConsumer;
import com.google.dart.server.internal.remote.RemoteAnalysisServerImpl;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.codeInsight.hints.presentation.PresentationRenderer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ScrambledOutputStream;
import com.intellij.util.concurrency.EdtExecutorService;
import com.jetbrains.lang.dart.DartLanguage;
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService;
import com.jetbrains.lang.dart.ide.documentation.DartDocumentationProvider;
import com.jetbrains.lang.dart.psi.DartCallExpression;
import com.jetbrains.lang.dart.psi.DartComponent;
import com.jetbrains.lang.dart.psi.DartRecursiveVisitor;
import com.jetbrains.lang.dart.psi.DartVarAccessDeclaration;
import org.dartlang.analysis.server.protocol.HoverInformation;
import org.dartlang.analysis.server.protocol.RequestError;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;




public class DartInlayHints extends AnAction implements Toggleable {


    Runnable Task;

    boolean State = false;

    boolean flag = false;



    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {


        State = !State;

        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        VirtualFile psiFileVirtualFile = psiFile.getVirtualFile();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        EditorImpl editorImpl = ObjectUtils.tryCast(editor, EditorImpl.class);



        iterateOverVariable(psiFile, psiFileVirtualFile,editorImpl);

        iterateOverMethod(psiFile, psiFileVirtualFile, editorImpl);

        //the main task for methods.
        psiFile.accept(new DartRecursiveVisitor() {

            @Override
            public void visitCallExpression(@NotNull DartCallExpression o) {
                super.visitCallExpression(o);
                if (o.getExpression().getLastChild() != null) {
                    var MethodElement = o.getExpression().getLastChild();
                    String Doc = new DartDocumentationProvider().generateDoc(MethodElement, MethodElement.getOriginalElement());
                    String methodInfo = getMethodInfo(Doc);
                    //System.out.println("methodInfo : " + methodInfo + "\n\n");

                    List<String> paramHints = extractParamsHints(methodInfo);

                    var argList = o.getArguments().getArgumentList();
                    if (argList != null) {
                        List<HoverInformation> hoverList = psiFileVirtualFile != null ? DartAnalysisServerService.getInstance(psiFile.getProject()).analysis_getHover(psiFileVirtualFile, o.getTextOffset()) : Collections.emptyList();
                        var information = hoverList.isEmpty() ? null : (HoverInformation)hoverList.get(0);

                        for (PsiElement child : argList.getChildren()) {

                            List<HoverInformation> hoverList1 = psiFileVirtualFile != null ? DartAnalysisServerService.getInstance(psiFile.getProject()).analysis_getHover(psiFileVirtualFile, child.getTextOffset()) : Collections.emptyList();
                            var information1 = hoverList1.isEmpty() ? null : (HoverInformation)hoverList1.get(0);





                            addInlayText(paramHints.get(0), child.getTextOffset(), editor,"func");
                            paramHints.remove(0);
                        }
                        if (!paramHints.isEmpty()) {
                            int startoffset = o.getArguments().getLastChild().getTextOffset();
                            addInlayText(paramHints.get(0), startoffset, editor,"func");
                        }
                    } else {
                        for (String paramHint : paramHints) {
                            int startOffset = o.getArguments().getLastChild().getTextOffset();
                            addInlayText(paramHint, startOffset, editor,"func");
                        }
                    }
                }
            }
        });


        Task = new Runnable() {
            @Override
            public void run() {
                if (!State){
                    return;
                }

            }
        };


/*
        if (!flag) {
            EdtExecutorService.getScheduledExecutorInstance().scheduleWithFixedDelay(Task, 1, 5, TimeUnit.SECONDS);
            flag = true;
        }
        if (!State){
            assert editor != null;
            for (Inlay<?> inlay : editor.getInlayModel().getInlineElementsInRange(0, editor.getDocument().getTextLength())) {
                inlay.dispose();
            }
        }
*/




    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        assert psiFile != null;
        e.getPresentation().setEnabledAndVisible(psiFile.getLanguage().is(DartLanguage.INSTANCE));
        //Toggleable.setSelected(e.getPresentation(), State);
        //update code is here
    }


    String extractVarType(PsiFile psiFile,VirtualFile psiFileVirtualFile, PsiElement element){
        List<HoverInformation> hoverList = psiFileVirtualFile != null ? DartAnalysisServerService.getInstance(psiFile.getProject()).analysis_getHover(psiFileVirtualFile, element.getTextOffset()) : Collections.emptyList();
        HoverInformation information = hoverList.isEmpty() ? null : (HoverInformation) hoverList.get(0);
        assert information != null;
        return information.getStaticType();
    }


    void iterateOverVariable(PsiFile psiFile,VirtualFile psiFileVirtualFile,EditorImpl editor){
        psiFile.accept(new DartRecursiveVisitor(){
            @Override
            public void visitVarAccessDeclaration(@NotNull DartVarAccessDeclaration varDeclaration) {
                if (varDeclaration.getOriginalElement().getText().startsWith("var")) {
                    assert editor != null;
                    String trueVarType = extractVarType(psiFile, psiFileVirtualFile, varDeclaration);
                    addInlayText(trueVarType, varDeclaration.getTextOffset(), editor,"var");
                }
            }
        });
    }




    void extractMethodParams(PsiFile psiFile,VirtualFile psiFileVirtualFile, PsiElement element){
        List<HoverInformation> hoverList = psiFileVirtualFile != null ? DartAnalysisServerService.getInstance(psiFile.getProject()).analysis_getHover(psiFileVirtualFile, element.getTextOffset()) : Collections.emptyList();
        HoverInformation information = hoverList.isEmpty() ? null : (HoverInformation) hoverList.get(0);
        String rawParams = information.getElementDescription();
        Pattern pattern = Pattern.compile("\\([\\w, ]+\\)");

        var parametersName =
            pattern.
                matcher(rawParams).
                results().
                filter(text -> !text.group().equals("(new)")).
                map(text -> text.group().replaceAll("[()]","")).
                collect(Collectors.joining(",")).split(",");


            ;



        assert information != null;


    }

    void iterateOverMethod(PsiFile psiFile,VirtualFile psiFileVirtualFile,EditorImpl editor){

        psiFile.accept(new DartRecursiveVisitor(){

            @Override
            public void visitCallExpression(@NotNull DartCallExpression callExpression) {
                extractMethodParams(psiFile, psiFileVirtualFile, callExpression);
            }
        });


    }


    String extractVarType(String varElementInfo) {
        var tempList = varElementInfo.split("Type:");
        return tempList[tempList.length - 1].trim();
    }


    String getMethodInfo(String RawDoc) {
        RawDoc = getElementInfo(RawDoc);
        var temp = RawDoc.split("\n", 2);
        String filteredMethodDoc = temp[temp.length - 1];
        return filteredMethodDoc;
    }

    String getElementInfo(String RawDoc) {
        RawDoc = Arrays.stream(RawDoc.split("</code>")).findFirst().get();
        RawDoc = RawDoc.replaceAll("<br>", "\n")
                .replaceAll("<b>", "")
                .replaceAll("</b>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("</code>", "")
                .replaceAll("<code>", "")
                .replaceAll("<pre>", "")
                .replaceAll("</pre>", "")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .strip().trim();
        String filteredDoc = Arrays
                .stream(RawDoc.split("\n"))
                .filter(line -> !line.equals(""))
                .collect(Collectors.joining("\n"));

        return filteredDoc;
    }

    List<String> extractParamsHints(String orgMethodInfo) {
        orgMethodInfo = Arrays.stream(orgMethodInfo.split("\n")).findFirst().get();
        var tempList = orgMethodInfo.split("\\(");
        orgMethodInfo = tempList[tempList.length - 1];
        orgMethodInfo = orgMethodInfo.replaceAll("\\)", "").trim();
        var ParamsHints = Arrays.stream(orgMethodInfo.split(",")).collect(Collectors.toList());
        ParamsHints = ParamsHints.stream().map(elem -> elem.trim().replaceAll(" ", ": ")).collect(Collectors.toList());

        return ParamsHints;
    }


    void addInlayText(String text, int offset, Editor editor,String kind) {

        var a = editor.getInlayModel().getInlineElementsInRange(offset, offset);
        if (!a.isEmpty()) {
            for (Inlay<?> inlay : a) {
                if (kind.equals("var") && !Objects.equals(inlay.getRenderer().toString(), text)){
                    inlay.dispose();
                }
                else if (Objects.equals(inlay.getRenderer().toString(), text)) return;
            }
        }

        EditorImpl editor1 = ObjectUtils.tryCast(editor, EditorImpl.class);


        var presentation = new PresentationFactory(editor1).smallText(text);
        editor.getInlayModel().addInlineElement(offset, true, new PresentationRenderer(
                new PresentationFactory(editor1).roundWithBackground(presentation)
        ));

    }


    String getPath(Editor editor) {
        StringBuilder path = new StringBuilder(editor.getDocument().toString());
        path = new StringBuilder(path.substring(20, path.length() - 1));
        var tempList = path.toString().split("/");
        var s = Arrays.stream(tempList).skip(tempList.length - 2).toArray();
        path = new StringBuilder();
        for (Object string : s) {
            path.append(string.toString()).append("/");
        }
        path.deleteCharAt(path.length() - 1);
        return path.toString();
    }

}



/*
        Task = new Runnable() {
@Override
public void run() {
    System.out.println(Thread.currentThread().getName());
    if (!State){
    return;
    }



    }
    };


    if (!flag) {
    EdtExecutorService.getScheduledExecutorInstance().scheduleWithFixedDelay(Task, 1, 5, TimeUnit.SECONDS);
    flag = true;
    }
    if (!State){
    assert editor != null;
    for (Inlay<?> inlay : editor.getInlayModel().getInlineElementsInRange(0, editor.getDocument().getTextLength())) {
    inlay.dispose();
    }
    }
*/


/*
 * System.out.println("o.getText() : "+o.getText()); // return the call signature as a text (useless)
 *
 *
 *
 *
 */


/*
 * Element(VAR_DECLARATION_LIST) for all assignments
 * PsiElement(IDENTIFIER)
 *
 * LeafPsiElement@42880 myText is (Type "int, List, double, etc..")
 *
 * Element(METHOD_DECLARATION)
 *
 * DartFieldFormalParameterImpl : is for method fields
 *
 * DartVarDeclarationListImpl is for the assignment statement (VAR_DECLARATION_LIST)
 * what i'm thinking of is searching for all vars and then do the magic from there.
 *
 * CompisuteElement Element(TYPE) Element(NEW_EXPRESSION)
 *
 * com.jetbrains.lang.dart.psi.impl.DartReferenceExpressionImpl : might be important in cases like var x = q; where q is assigned above
 *
 *
 * VarDeclarationList Must contain VarAccessList and Var_INIT to be Valid in our case
 * */



        /*
        PsiReference reference = psiFile.getReference();
        //System.out.println(PsiTreeUtil.getParentOfType(psiElement, PsiVariable.class).getName());
        System.out.println(reference.getElement().getText());
*/


//var s = psiFile.getNode().findChildByType(new DartElementType("int"));

        /*
        for (var x : editor.getInlayModel().getInlineElementsInRange(0, editor.getDocument().getTextLength())) {
            System.out.println("renderer: " + x.getRenderer());
            System.out.println("placement: " + x.getPlacement());
            System.out.println("offset: " + x.getOffset());
        }


        for (var element : psiFile.getChildren()) {
            psiFile.getNode();
        }
        */

        /*
        for (var element : psiFile.getChildren()){
            // get the range of the element
            System.out.println("start offset :"+element.getNode().getTextRange().getStartOffset());
            System.out.println("end offset :"+ element.getNode().getTextRange().getEndOffset());
        }
        */


// the adding of inlay to editor
//var render = editor.getInlayModel().getInlineElementsInRange(0, editor.getDocument().getTextLength()).get(1).getRenderer();

        /*
        String text = "Kh";
       editor.getInlayModel().addInlineElement(2, false,new EditorCustomElementRenderer() {
           @Override
           public int calcWidthInPixels(@NotNull Inlay inlay) {
               return text.length()*9;
           }

           @Override
           public int calcHeightInPixels(@NotNull Inlay inlay) {
               return EditorCustomElementRenderer.super.calcHeightInPixels(inlay);
           }

           @Override
           public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
               g.setColor(JBColor.GRAY);
               g.drawString(text, targetRegion.x, targetRegion.y+16);
           }
       });
*/

        /*
        System.out.println(psiFile.getChildren()[2].getText());

        for (PsiElement child : psiFile.getChildren()[2].getChildren()){
            System.out.println("Type:" + child.toString());
            System.out.println("---------");
        }
*/

        /*
        var path = editor.getDocument().toString();
        path = path.substring(20, path.length()-1);

        try {
            FileWriter w =new FileWriter(path);
            w.write(editor.getDocument().getText()+"\nxxxx");
            w.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        */

/*
        editor.getCaretModel().getAllCarets().forEach(System.out::println);
        for (FoldRegion allFoldRegion : editor.getFoldingModel().getAllFoldRegions()) {
            System.out.println("1- "+ allFoldRegion.getEditor().getDocument().getText());
            System.out.println("**********************************************************");
        }
*/






        /*
        psiFile.accept(new DartRecursiveVisitor() {
            @Override
            public void visitVarDeclarationList(@NotNull DartVarDeclarationList o) {
                super.visitVarDeclarationList(o);

                    o.getVarAccessDeclaration().accept(new DartRecursiveVisitor(){
                        boolean foundType = false;
                        @Override
                        public void visitElement(@NotNull PsiElement element) {
                            if (foundType) return;
                            try {
                                super.visitElement(element);
                                if (element instanceof LeafPsiElement){
                                    System.out.println(element.getText());
                                    foundType = true;
                                }
                            }catch (Exception e){}
                        }
                    });


            }
        });
*/






                /*
                psiFile.accept(new DartRecursiveVisitor(){

                    @Override
                    public void visitVarAccessDeclaration(@NotNull DartVarAccessDeclaration o) {

                        var s = o.getComponentName().getOriginalElement();
                        var file = o.getContainingFile();
                        var vFile = file.getVirtualFile();


                        LinkedList<HoverInformation> a = new LinkedList<>();
                        var x = new GetHoverConsumer(){
                            @Override
                            public void computedHovers(HoverInformation[] hovers) {
                                Collections.addAll(a, hovers);
                            }

                            @Override
                            public void onError(RequestError requestError) {

                            }
                        };

                        try {
                            RemoteAnalysisServerImpl.class.getDeclaredMethod("analysis_getHover",String.class,int.class,GetHoverConsumer.class );
                        } catch (NoSuchMethodException ex) {
                            throw new RuntimeException(ex);
                        }

                        List<HoverInformation> hoverList = vFile != null ? DartAnalysisServerService.getInstance(psiFile.getProject()).analysis_getHover(vFile, s.getTextOffset()) : Collections.emptyList();
                        var information = hoverList.isEmpty() ? null : (HoverInformation)hoverList.get(0);

                        super.visitVarAccessDeclaration(o);
                    }
                });

                */


// main task is here for var
                /*
                psiFile.accept(new DartRecursiveVisitor() {

                    @Override
                    public void visitComponent(@NotNull DartComponent o) {
                        super.visitComponent(o);

                        if (o.getOriginalElement() instanceof DartVarAccessDeclaration && o.getParent().getNextSibling().getText().equals(";")){

                            try {


                                List<HoverInformation> hoverList = psiFileVirtualFile != null ? DartAnalysisServerService.getInstance(psiFile.getProject()).analysis_getHover(psiFileVirtualFile, o.getTextOffset()) : Collections.emptyList();
                                var information = hoverList.isEmpty() ? null : (HoverInformation)hoverList.get(0);



                                String Doc = new DartDocumentationProvider().generateDoc(o, o.getOriginalElement());
                                assert Doc != null;
                                String elementInfo = getElementInfo(Doc);
                                String TrueElementType = extractVarType(elementInfo);
                                if (o.getOriginalElement().getText().startsWith("var")) {
                                    assert editor != null;
                                    addInlayText(TrueElementType, o.getTextOffset(), editor,"var");
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                });

                */