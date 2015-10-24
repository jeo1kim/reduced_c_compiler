
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by jinyongsuk on 10/8/15.
 */
public abstract class ArithmeticOp extends BinaryOp {
    // Name of the Type (e.g., int, bool, some structdef, etc.)
    private String m_OpName;
    //private int m_size;

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public ArithmeticOp(String strName )
    {
        super(strName);
        setName(strName);
        //setSize(size);
    }

    STO checkOperands(STO a, STO b, String opName)
    {
        if (b.isError()){
            return b;
        }if (a.isError()){
            return a;
        }
        //System.out.println(a.getType().isNumeric());
        Type aType = a.getType();
        Type bType = b.getType();

        if( !(aType.isNumeric()))
        {
            return new ErrorSTO(Formatter.toString(ErrorMsg.error1n_Expr, aType.getName(),opName));
        }
        if(!(bType.isNumeric()))
        {
            return new ErrorSTO(Formatter.toString(ErrorMsg.error1n_Expr, bType.getName(), opName));
        }
        // if both are const do the calculation
        if (a.isConst() && b.isConst()) {
            // need to check float <=> int
            if(opName == "/") {
                if ((b.getType().isInt() && b.getIntValue() == 0) || (b.getType().isFloat() && b.getFloatValue() == 0.0)) {
                    return new ErrorSTO(ErrorMsg.error8_Arithmetic);
                }
            }
            // do the calculation
            BigDecimal result = calculate(a.getValue(), b.getValue(), opName);
            if (aType.isInt() && bType.isInt()) {
                ConstSTO ret = new ConstSTO( Integer.toString(result.intValue()), aType, result.intValue());
                ret.markRVal();
                return ret;
            }
            // when one of them are float return float
            else if((aType.isInt() && bType.isFloat()) || (aType.isFloat() && bType.isInt())) {
                STO c = !(aType instanceof intType) ? a : b;
                ConstSTO ret = new ConstSTO(Double.toString(result.doubleValue()), c.getType(), result.doubleValue());
                ret.markRVal();
                return ret;
            }
            else{
                ConstSTO ret = new ConstSTO( Double.toString(result.doubleValue()), aType, result.doubleValue());
                ret.markRVal();
                return ret;
            }
        }
        else{
            // case where one of the expression is not a Const
            STO c = !(aType instanceof intType) ? a : b;
            //BigDecimal result = calculate(a.getValue(), b.getValue(), opName);
            ExprSTO ret = new ExprSTO(a.getName()+b.getName(), c.getType());
            ret.markRVal();
            return ret;
        }
    }

    public BigDecimal calculate (BigDecimal a, BigDecimal b, String opname){

        BigDecimal result = null;

        switch (opname) {
            case "+": result = a.add(b);
                    break;
            case "-": result =  a.subtract(b);
                    break;
            case "*": result = a.multiply(b);
                    break;
            case "/": result = a.divide(b, 1, RoundingMode.CEILING);


                break;
        }

        return result;
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public String getName()
    {
        return m_OpName;
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    private void setName(String str)
    {
        m_OpName = str;
    }
}
