//---------------------------------------------------------------------
// CSE 131 Reduced-C Compiler Project
// Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
// University of California, San Diego
//---------------------------------------------------------------------


import java_cup.runtime.*;

import javax.swing.text.Style;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import java.lang.*;


class MyParser extends parser {
    private Lexer m_lexer;
    private ErrorPrinter m_errors;
    private boolean m_debugMode;
    private int m_nNumErrors;
    private String m_strLastLexeme;
    private boolean m_bSyntaxError = true;
    private int m_nSavedLineNum;

    private SymbolTable m_symtab;

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public MyParser(Lexer lexer, ErrorPrinter errors, boolean debugMode) {
        m_lexer = lexer;
        m_symtab = new SymbolTable();
        m_errors = errors;
        m_debugMode = debugMode;
        m_nNumErrors = 0;
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public boolean Ok() {
        return m_nNumErrors == 0;
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public Symbol scan() {
        Token t = m_lexer.GetToken();

        //	We'll save the last token read for error messages.
        //	Sometimes, the token is lost reading for the next
        //	token which can be null.
        m_strLastLexeme = t.GetLexeme();

        switch (t.GetCode()) {
            case sym.T_ID:
            case sym.T_ID_U:
            case sym.T_STR_LITERAL:
            case sym.T_FLOAT_LITERAL:
            case sym.T_INT_LITERAL:
                return new Symbol(t.GetCode(), t.GetLexeme());
            default:
                return new Symbol(t.GetCode());
        }
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public void syntax_error(Symbol s) {
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public void report_fatal_error(Symbol s) {
        m_nNumErrors++;
        if (m_bSyntaxError) {
            m_nNumErrors++;

            //	It is possible that the error was detected
            //	at the end of a line - in which case, s will
            //	be null.  Instead, we saved the last token
            //	read in to give a more meaningful error
            //	message.
            m_errors.print(Formatter.toString(ErrorMsg.syntax_error, m_strLastLexeme));
        }
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public void unrecovered_syntax_error(Symbol s) {
        report_fatal_error(s);
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public void DisableSyntaxError() {
        m_bSyntaxError = false;
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public void EnableSyntaxError() {
        m_bSyntaxError = true;
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public String GetFile() {
        return m_lexer.getEPFilename();
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public int GetLineNum() {
        return m_lexer.getLineNumber();
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public void SaveLineNum() {
        m_nSavedLineNum = m_lexer.getLineNumber();
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public int GetSavedLineNum() {
        return m_nSavedLineNum;
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    void DoProgramStart() {
        // Opens the global scope.

        m_symtab.openScope();
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    void DoProgramEnd() {
        m_symtab.closeScope();
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    void DoVarDecl(String id) {
        if (m_symtab.accessLocal(id) != null)
            if (m_symtab.accessLocal(id) != null) {
                m_nNumErrors++;
                m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
            }

        VarSTO sto = new VarSTO(id);
        m_symtab.insert(sto);
    }

    void DoVarDeclwType(String id, Type typ, boolean stat, Vector<STO> array, STO init) {
        if (init != null && init.isError()) {
            m_nNumErrors++;
            return;    // might wanan change with !init.isError()
        }
        if (m_symtab.accessLocal(id) != null) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
            return;
        }


        VarSTO sto = new VarSTO(id, typ);
        if (stat) {
            sto.setStatic(stat); // set Variable static
        }
        if (array.size() == 0 && (init != null)) { // indicates that this var is not an array and init exp exist
            // do the type check with init if it exist
            Type initType = init.getType();
            if(init.getType().isArray()){
                init.getType().getNextType();
            }

            if (!initType.isAssignableTo(sto.getType())) {
                m_nNumErrors++;
                m_errors.print(Formatter.toString(ErrorMsg.error8_Assign, getTypeName(init), getTypeName(typ)));
                return;
            } else { // exp is assignable to this varSto type. so
                sto.setValue(init.getValue()); // set the value
                m_symtab.insert(sto);
                return;
            }
        }
        //case where var is an array
        else if (array.size() > 0) {
            VarSTO ret = new VarSTO(id, new ArrayType("", array.size()));
            Type arrType = new ArrayType("", 0);
            Type temp;
            for (STO arr : array) {
                if (arr.isError()){
                    m_nNumErrors++;
                    return;
                }
                if (!(arr.getType().isInt())) {
                    m_nNumErrors++;
                    m_errors.print(Formatter.toString(ErrorMsg.error10i_Array, getTypeName(arr)));
                    return;
                } else if (!arr.isConst()) {
                    m_nNumErrors++;
                    m_errors.print(ErrorMsg.error10c_Array);
                    return;
                } else if (arr.getIntValue() <= 0) {
                    m_nNumErrors++;
                    m_errors.print(Formatter.toString(ErrorMsg.error10z_Array, arr.getIntValue()));
                    return;
                }
            }

            temp = DoArrayType(array, typ, arrType, 0);
            ret.setType(temp);
            ret.markModLVal();
            m_symtab.insert(ret);
            return;
        } else {
            m_symtab.insert(sto);
            return;
        }
    }

    STO makeArrayParam(Type typ, boolean ref, String id, Vector<STO> array){

        VarSTO ret = new VarSTO(id, typ, ref);
        if ( array.size() > 0){
            Type arrType = new ArrayType("", 0);
            Type temp;
            for (STO arr : array) {
                if (arr.isError()){
                    m_nNumErrors++;
                    return new ErrorSTO("makeArrayParam");
                }
                if (!(arr.getType().isInt())) {
                    m_nNumErrors++;
                    m_errors.print(Formatter.toString(ErrorMsg.error10i_Array, getTypeName(arr)));
                    return new ErrorSTO("makeArrayParam");
                } else if (!arr.isConst()) {
                    m_nNumErrors++;
                    m_errors.print(ErrorMsg.error10c_Array);
                    return new ErrorSTO("makeArrayParam");
                } else if (arr.getIntValue() <= 0) {
                    m_nNumErrors++;
                    m_errors.print(Formatter.toString(ErrorMsg.error10z_Array, arr.getIntValue()));
                    return new ErrorSTO("makeArrayParam");
                }
            }

            temp = DoArrayType(array, typ, arrType, 0);
            ret.setType(temp);
            ret.markModLVal();
        }

        return ret;
    }

    Type DoArrayType(Vector<STO> array, Type base, Type arrType, int n) {

        if (n == array.size()-1) {
            arrType.setNextType(base); // if base case my next type is the base type
            arrType.setName(base.getName() + "[" + array.get(n).getName() + "]");
            arrType.setSize(array.get(array.size() - 1).getIntValue());
            return arrType;
        }
        Type type = new ArrayType(  "[" + array.get(n).getName() + "]" +arrType.getName(), array.get(n).getIntValue());
        arrType.setSize(array.get(n).getIntValue());
        arrType.setNextType(DoArrayType(array, base, type, n + 1));

        arrType.setName(base.getName() + type.getName());
        return arrType;

    }

    void DoVarDeclwAuto(String id, STO expr, boolean stat) {
        if (expr.isError()) {
            return;
        }
        if (m_symtab.accessLocal(id) != null) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
        }

        if (!expr.isConst()) {
            VarSTO sto = new VarSTO(id, expr.getType());
            if (stat) {
                sto.setStatic(stat); // set variable static
            }
            m_symtab.insert(sto);

        } else {
            ConstSTO sto = new ConstSTO(id, expr.getType());
            if (stat) {
                sto.setStatic(stat); // set variable static
            }
            m_symtab.insert(sto);
        }

        //m_symtab.insert(sto);
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    void DoExternDecl(String id) {
        if (m_symtab.accessLocal(id) != null) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
        }

        VarSTO sto = new VarSTO(id);
        m_symtab.insert(sto);
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    void DoConstDecl(String id, Type typ, STO exp, boolean stat) {

        if (exp != null && exp.isError()) {
            m_nNumErrors++;
            return;    // might wanan change with !init.isError()
        }
        if (m_symtab.accessLocal(id) != null) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
        }

        ConstSTO sto = new ConstSTO(id, typ);   // fix me
        if (!exp.isConst()) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.error8_CompileTime, id));
            return;
        }

        if (!exp.getType().isAssignableTo(typ)) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.error8_Assign, getTypeName(exp), getTypeName(typ)));
            return;
        } else { // exp is assignable to this varSto type. so
            if (exp.getValue() == null) {
                m_nNumErrors++;
                m_errors.print(Formatter.toString(ErrorMsg.error8_CompileTime, id));
                return;
            }
            if (stat) {
                sto.setStatic(stat); // set Variable static
            }
            sto.setValue(exp.getValue());
            sto.markModLVal();
            m_symtab.insert(sto);
            return;
        }


    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    void DoConstDeclwAuto(String id, STO exp, boolean stat) {
        if (exp != null && exp.isError()) {
            m_nNumErrors++;
            return;    // might wanan change with !init.isError()
        }
        if (m_symtab.accessLocal(id) != null) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
        }

        ConstSTO sto = new ConstSTO(id, exp.getType());   // fix me
        if (stat) {
            sto.setStatic(stat); // set Variable static
        }
        if (exp.getValue() == null) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.error8_CompileTime, id));
            return;
        }
        sto.setValue(exp.getValue());
        sto.markModLVal();
        m_symtab.insert(sto);
        return;
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    void DoStructdefDecl(String id) {
        if (m_symtab.accessLocal(id) != null) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
        }

        StructdefSTO sto = new StructdefSTO(id);
        m_symtab.insert(sto);
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    void DoFuncDecl_1(String id) {
        boolean isThereOverloadedFunction = false;
        STO a = m_symtab.accessLocal(id);

        if (a != null && !(a.isFunc())) //if found STO is not function
        {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
            return;
            //error9_Decl
            // "Duplicate declaration of overloaded function %S.";
        }

        FuncSTO sto = new FuncSTO(id);


        m_symtab.insert(sto);
        //m_symtab.insertOverloadedFunc(id, sto); //all funcSTO goes into HashMap
        m_symtab.openScope();
        m_symtab.setFunc(sto);
        //sto.setLevel(m_symtab.getLevel());
    }

    //func decl
    void DoFuncDecl_1_param(String id, Type ret, Vector<STO> params) {
        boolean isThereOverloadedFunction = false;
        STO a = m_symtab.accessLocal(id);
        if (a != null && !(a.isFunc())) //if found STO is not function
        {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
        }

        String key = makeHKey(id, params);

        FuncSTO sto = new FuncSTO(id, ret, params); //

        if (a != null && a.isFunc()) {  // function exist check for overload.
            FuncSTO exist = (FuncSTO) a;

            if (exist.getOverloaded(key) != null) { // overload function doesnt exist add.
                m_nNumErrors++;
                m_errors.print(Formatter.toString(ErrorMsg.error9_Decl, id));
            } else { // function exist throw error
                exist.addOverload(key, sto);
            }
        }
        sto.addOverload(key, sto);

        m_symtab.insert(sto);
        //m_symtab.insertOverloadedFunc(hKey, sto); //all funcSTO goes into HashMap
        m_symtab.openScope();
        m_symtab.setFunc(sto);

    }

    // funcdef
    void DoFuncDecl_1_param(String id, Type ret, boolean ref, Vector<STO> params) {

        STO a = m_symtab.accessLocal(id);

        if (a != null && !a.isFunc()) //if found STO is not function
        {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.redeclared_id, id));
        }


        String key = makeHKey(id, params);

        FuncSTO sto = new FuncSTO(id, ret, params, ref); //

        if (a != null && a.isFunc()) {  // function exist check for overload.
            FuncSTO exist = (FuncSTO) a;

            if (exist.getOverloaded(key) != null) { // overload function doesnt exist add.
                m_nNumErrors++;
                m_errors.print(Formatter.toString(ErrorMsg.error9_Decl, id));
            } else { // function exist throw error
                exist.addOverload(key, sto);
            }
        }
        sto.addOverload(key, sto);

        m_symtab.insert(sto);
        m_symtab.openScope();
        m_symtab.setFunc(sto);
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    void DoFuncDecl_2() {

        m_symtab.closeScope();
        m_symtab.setFunc(null);
    }


    //----------------------------------------------------------------
    // Method for Function parameter checking
    //----------------------------------------------------------------
    void DoFormalParams(Vector<STO> params) {
        FuncSTO func = m_symtab.getFunc();
        //System.out.print(params.get(0));
        if (func == null) {
            m_nNumErrors++;
            m_errors.print("internal: DoFormalParams says no proc!");
            return;
        }
        for (STO param : params) {
            m_symtab.insert(param);
        }
        func.setParamVec(params);
        func.setParamCount(params.size()); // set the

    }

    //----------------------------------------------------------------
    // Opens the Scope, global, function, brackets.
    //----------------------------------------------------------------
    void DoBlockOpen() {
        // Open a scope.

        m_symtab.openScope();
        //m_symtab.setScopeName(sName);
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    void DoBlockClose() {
        m_symtab.closeScope();
        //m_symtab.setScopeName(null);
    }


    void ForeachCheck(Type type, boolean ref, String id, STO expr) {

        if (!expr.getType().isArray()) {
            m_nNumErrors++;
            m_errors.print(ErrorMsg.error12a_Foreach);
            return;
        } else if (!ref && !expr.getType().getNextType().isAssignableTo(type)) {
            m_nNumErrors++;
            //      "Foreach array element of type %T not assignable to value iteration variable %S, of type %T.";

            m_errors.print(Formatter.toString(ErrorMsg.error12v_Foreach, getTypeName(expr.getType().getNextType()), id, getTypeName(type)));
            return;
        } else if (ref && !expr.getType().getNextType().isEquivalentTo(type)) {
            m_nNumErrors++;
            //      "Foreach array element of type %T not equivalent to reference iteration variable %S, of type %T.";
            m_errors.print(Formatter.toString(ErrorMsg.error12r_Foreach, getTypeName(expr.getType().getNextType()), id, getTypeName(type)));
            return;
        }


    }

    void pushLoop(String name) {
        m_symtab.pushLoop(name);
    }

    void popLoop() {
        m_symtab.popLoop();
    }

    void BreakorCont(String borc) {

        int size = m_symtab.getLoopSize();
        if (size == 0) {
            if (borc == "break") {
                m_nNumErrors++;
                m_errors.print(ErrorMsg.error12_Break);
                return;
            } else {
                m_nNumErrors++;
                m_errors.print(ErrorMsg.error12_Continue);
                return;
            }
        }
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    STO DoAssignExpr(STO stoDes, STO expr) {
        if (expr.isError()) {
            m_nNumErrors++;
            return expr;
        }
        if (stoDes.isError()) {
            m_nNumErrors++;
            return stoDes;
        }
        if (!stoDes.isModLValue()) {
            if (expr instanceof ErrorSTO) {
                return new ErrorSTO(ErrorMsg.error3a_Assign);
            }
            m_nNumErrors++;
            //      "Left-hand operand is not assignable (not a modifiable L-value).";
            m_errors.print(ErrorMsg.error3a_Assign);
//			STO result = new ExprSTO(stoDes.getName()+expr.getName(), expr.getType());
//			result.markRVal();
//			return result;
            // Good place to do the assign checks
            return new ErrorSTO(ErrorMsg.error3a_Assign);
        }
        if (!expr.getType().isAssignableTo(stoDes.getType())) {

            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.error3b_Assign, getTypeName(expr), getTypeName(stoDes)));
            return new ErrorSTO(ErrorMsg.error3a_Assign); // do we need this?
        }
        //error3b_Assign ="Value of type %T not assignable to variable of type %T.";

        return stoDes;
    }


    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    STO DoFuncCall(STO sto, Vector<STO> argTyp) {
        // recursive case? calling main inside main;
        FuncSTO recurFunc = m_symtab.getFunc();

        if (sto.isError()) {
            return sto;
        }
        // func holds expected param
        FuncSTO func = (FuncSTO) m_symtab.access(sto.getName());

        String hKey = makeHKey(sto.getName(), argTyp); //u can use it for anyother func call
        Vector<STO> paramList;
        //if func is OverLoaded Function then check 9b
        if (func.m_overLoadFuncName.size() > 1) {
            if (func != null && func.getOverloaded(hKey) == null) {
                m_nNumErrors++;
                m_errors.print(Formatter.toString(ErrorMsg.error9_Illegal, func.getName()));
                return new ErrorSTO(sto.getName());
            }
            func = func.getOverloaded(hKey);
        }

        paramList = func.getParamVec();
        //<--- error case if undeclared function-call then


        //if func is not Overloaded then check 5

        if (!sto.isFunc()) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.not_function, sto.getName()));
            return new ErrorSTO(sto.getName());
        } else if (!func.isFunc() || func == null) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.not_function, func.getName()));
            return new ErrorSTO(sto.getName());
        } else if (argTyp.size() != func.getParamCount()) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.error5n_Call, argTyp.size(), func.getParamCount()));
            return new ErrorSTO(sto.getName());
        }


        //chech 0 parm
        // paramType has arguments
        Iterator<STO> it1;
        Iterator<STO> it2;
        Boolean flag = false;
        for (it1 = argTyp.iterator(), it2 = paramList.iterator(); it1.hasNext() && it2.hasNext(); ) {  //VarSTO params : paramTyp && (VarSTO argTyp : ((FuncSTO) func).setParamVec();)){


            STO arg = it1.next();
            STO param = it2.next();

            if (arg.isError()) {
                return arg;
            }
            if (!param.isRef() && !arg.getType().isAssignableTo(param.getType())) {
                m_nNumErrors++;

                m_errors.print(Formatter.toString(ErrorMsg.error5a_Call, getTypeName(arg), param.getName(), getTypeName(param)));
                flag = true;

            }
            if (param.isRef()) {
                if (!arg.getType().isEquivalentTo(param.getType())) {
                    m_nNumErrors++;
                    m_errors.print(Formatter.toString(ErrorMsg.error5r_Call, getTypeName(arg), param.getName(), getTypeName(param)));
                    flag = true;

                }//return new ErrorSTO(sto.getName());
                else if (!arg.isModLValue()) {
                    m_nNumErrors++;
                    m_errors.print(Formatter.toString(ErrorMsg.error5c_Call, param.getName(), getTypeName(param)));
                    flag = true;
                    //return new ErrorSTO(sto.getName());
                }
            }
        }
        if (flag) {
            return new ErrorSTO(sto.getName());
        }

        ExprSTO ret = new ExprSTO(sto.getName(), func.getType());
        // check if func sto was called by ref and assign R val or mod l val
        if (func.isRef()) {
            ret.markModLVal();
            ret.setRef(true);
            return ret;
        } else if (!func.isRef()) {
            ret.markRVal();
            return ret;
        }
        System.out.println("here");
        return sto;

    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    STO DoDesignator2_Dot(STO sto, String strID) {
        // Good place to do the struct checks

        return sto;
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    STO DoDesignator2_Array(STO sto, STO expr) {
        // Good place to do the array checks
        if (expr.isError()){
            return expr;
        }
        if (sto.isError()) {
            return sto;
        }

        if (!sto.getType().isArray()) { // add pointer type
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.error11t_ArrExp, getTypeName(sto)));
            return new ErrorSTO(sto.getName());
        }
        if (!expr.getType().isInt()) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.error11i_ArrExp, expr.getType().getName()));
            return new ErrorSTO(sto.getName());
        }
//        System.out.println(expr.getName());
//        System.out.println(expr.getIntValue());


        if (expr.isConst()){
            int ex = expr.getIntValue();
            int des = sto.getType().getSize()-1;
            if(ex > des ) {
                m_nNumErrors++;
                m_errors.print(Formatter.toString(ErrorMsg.error11b_ArrExp, expr.getIntValue(), sto.getType().getSize()));
                return new ErrorSTO(sto.getName());
            }
            if(ex<0){
                m_nNumErrors++;
                m_errors.print(Formatter.toString(ErrorMsg.error11b_ArrExp, expr.getIntValue(), sto.getType().getSize()));
                return new ErrorSTO(sto.getName());
            }
        }
        VarSTO ret = new VarSTO(sto.getName(), sto.getType().getNextType());
        ret.setValue(expr.getValue());
        return ret;
    }



    //----------------------------------------------------------------
    //	CASE: when there is no :: for assigning value to left
    //----------------------------------------------------------------
    STO DoDesignator3_ID(String strID) {
        STO sto;
        //check variable name in local scope
        if ((sto = m_symtab.accessLocal(strID)) == null) {
            //if there is not variable name in local scope
            //	then check the same name in global scope thus if u find
            //	then return the global scope

            if ((sto = m_symtab.accessGlobal(strID)) == null) {
                if ((sto = m_symtab.access(strID)) == null) {
                    m_nNumErrors++;
                    m_errors.print(Formatter.toString(ErrorMsg.undeclared_id, strID));
                    sto = new ErrorSTO(strID);
                }
            }

        }
        return sto;
    }

    //----------------------------------------------------------------
    // CASE: when there is :: for accessing global scope
    //----------------------------------------------------------------
    Type DoStructType_ID(String strID) {
        STO sto;

        if ((sto = m_symtab.access(strID)) == null) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.undeclared_id, strID));
            return new ErrorType();
        }

        if (!sto.isStructdef()) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.not_type, sto.getName()));
            return new ErrorType();
        }

        return sto.getType();
    }

    STO DoConditionCheck(STO condition) {
        Type conType = condition.getType();
        if (condition.isError()) {
            return condition;
        }
        if (!conType.isBool()) {

            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.error4_Test, conType.getName()));
            return new ErrorSTO(condition.getName());
        }
        return condition;
    }

    STO CheckGlobalColonColon(String strID) {
        STO sto;
        if ((sto = m_symtab.accessGlobal(strID)) == null) {
            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.error0g_Scope, strID));
            return new ErrorSTO(strID);
        }
        return sto;
    }


    STO DoBinaryExpr(STO a, Operator o, STO b) {
        if (b.isError()) {
            return b;
        }
        if (a.isError()) {
            return a;
        }
        STO result = o.checkOperands(a, b);
        if (result.isError()) {
            m_nNumErrors++;
            m_errors.print(result.getName());
            return new ErrorSTO(result.getName());
        }
        return result;
    }


    STO DoIncDecOp(STO a, Operator o) {
        STO result = o.checkOperands(a);

        if (a.isError()) {
            return a;
        }
        if (result.isError()) {
            m_nNumErrors++;
            m_errors.print(result.getName());
            return new ErrorSTO(result.getName());
        }

        return result;
    }

    STO MarkUnary(String unary, STO a) {

        if (a.getType().isNumeric() || a.isConst() || a.isExpr() || a.isVar()) {
            if (unary == "-") {
                a.setName(a.getName());
                a.setType(a.getType());
                a.setValue(a.getValue().negate());
                return a;
            } else {
                a.setName(a.getName());
                a.setType(a.getType());
                return a;
            }

        }

        return new ErrorSTO("error in mark unary");
    }

    STO DoUnaryOp(STO a, Operator o) {

        if (a.isError()) {
            return new ErrorSTO("DoUnaryOp" + a.getName() + o.getName());
        }
        STO result = o.checkOperands(a);
        if (result.isError()) {
            m_nNumErrors++;
            m_errors.print(result.getName());
            return new ErrorSTO("DoUnaryOp" + a.getName() + o.getName());
        }

        return result;
    }

    // this function is called on void return type functions
    STO DoVoidReturn() {
        FuncSTO result = m_symtab.getFunc();

        if (!(result.getType().isVoid())) {
            m_nNumErrors++;
            m_errors.print(ErrorMsg.error6a_Return_expr);
            return new ErrorSTO(result.getName());
        } else {
            //m_symtab.setFunc(null);
            return new ExprSTO(result.getName()); //
        }
    }

    STO DoExprReturn(STO a) {
        if (a.isError()) {
            return a;
        }
        FuncSTO result = m_symtab.getFunc();
//        if (a == result) {
//
//            return a;
//        }

        Type resultType = result.getType();
        Type exprType = a.getType();

        //type check pass by value
        if (!result.isRef()) {
            //if type is different but is assignable ex) int to float
            if (!exprType.isAssignableTo(resultType)) {
                //error6a_Return_type =
                //"Type.Type of return expression (%T), not assignment compatible with function's return type (%T).";
                m_nNumErrors++;
                m_errors.print(Formatter.toString(ErrorMsg.error6a_Return_type,
                        getTypeName(a), getTypeName(resultType)));

                return new ErrorSTO(a.getName());
            } else {
                //m_symtab.setFunc(null);
                return new ExprSTO(result.getName());
            }


        } else if (result.isRef()) // sane check
        //pass by reference
        //the type of the return expression is not equivalent to the return type of the function
        {
            if (!resultType.isEquivalentTo(exprType)) {  //resultType != exprType) {
                //error6b_Return_equiv =
                //"Type.Type of return expression (%T) is not equivalent to the function's return type (%T).";
                if (a.isRef()) {
                    m_nNumErrors++;
                    m_errors.print(Formatter.toString(ErrorMsg.error6b_Return_equiv,
                            getTypeName(a) + "*", getTypeName(result)));
                } else {
                    m_nNumErrors++;
                    m_errors.print(Formatter.toString(ErrorMsg.error6b_Return_equiv,
                            getTypeName(a), getTypeName(result)));
                }
                return new ErrorSTO("DoExprReturn" + a.getName());
            } else if (!(a.isModLValue())) {
                //error6b_Return_modlval =
                //		"Return expression is not a modifiable L-value for function that returns by reference.";
                m_nNumErrors++;
                m_errors.print(ErrorMsg.error6b_Return_modlval);
                return new ErrorSTO(a.getName());
            } else {
                //System.out.println("clearing func2");
                //m_symtab.setFunc(null);
                ExprSTO ret = new ExprSTO("result of " + result.getName());
                ret.setRef(true);
                return ret;
            }


        }
        System.out.println("In DoExpReturn this should never reach");
        m_symtab.setFunc(null);
        return new ExprSTO(result.getName());
    }


    void DoNoReturn(Vector<String> stmts) {

        FuncSTO result = m_symtab.getFunc();
        Type resultType = result.getReturnType();

        //if there is no ReturnType in Top-level
        boolean flag = false;
        for (String ret : stmts) {
            if ((ret != null) && (ret == "return")) {
                flag = true;
            }
        }

        if (!resultType.isVoid() && !flag) {
            m_nNumErrors++;
            m_errors.print(ErrorMsg.error6c_Return_missing);
            return;
        } else {
            return;
        }
    }


    STO DoExitExpr(STO a) {
        if (a.isError()) {
            return a;
        }
        Type aType = a.getType();

        if (!(aType.isAssignableTo(new intType("int", 4)))) {
            //error7_Exit  =
            //"Exit expression (type %T) is not assignable to int.";

            m_nNumErrors++;
            m_errors.print(Formatter.toString(ErrorMsg.error7_Exit, aType.getName()));
            return new ErrorSTO(a.getName());
        }

        //if assignable to int then return expr
        // double check what to return when you exit
        return new ExprSTO(a.getName());
    }

    // Helper Function
    String getTypeName(Type typ) {
        return typ.getName();
    }

    String getTypeName(STO sto) {
        return sto.getType().getName();
    }

    //its for making HashMap Key
    String makeHKey(String id, Vector<STO> param) {
        String paramKey = "";

        //set up H_Map key
        if (param != null) {
            for (STO para : param) {

                paramKey += "." + para.getType().getName();
            }
        }
        return id + paramKey;
    }

    STO markAmpersand(STO expr) {
        if (expr.isError()) {
            return expr;
        }

        expr.setRef(true);
        return expr;
    }


}
