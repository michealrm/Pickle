package pickle;

import java.util.HashMap;
import pickle.exception.InvalidNumberException;
import pickle.exception.InvalidOperationException;

public class ResultValue
{
	public SubClassif iDatatype;
	public Object value;
	public boolean isNumber = false;
	public String scTerminatingStr;
	
	// Constructor
	public ResultValue() {}
	
	
	/**
	* Start classifying variables and perform certain operations.
	* <p>
	* @param iType: the datatype of the object
	* @param val: the value of the object
	*/
	public ResultValue(SubClassif iType, Object val) throws Exception
	{
		iDatatype = iType;
		value = val;
		
		// Check if type is a number type
		if (iDatatype == SubClassif.INTEGER || iDatatype == SubClassif.FLOAT)
		{
			if (val instanceof Numeric)
			{
				value = val;
			}
			else
			{
				value = new Numeric( (String) val, iDatatype);
			}
			isNumber = true;
		}
		
		// Check if type is an identifier type
		else if (iDatatype == SubClassif.IDENTIFIER)
		{
			if (val instanceof Numeric)
			{
				value = val;
				iDatatype = ((Numeric) val).type;
			}
			else if (val instanceof String)
			{
				value = val;
				iDatatype = SubClassif.STRING;
			}
			else if (val instanceof Boolean)
			{
				value = val;
				iDatatype = SubClassif.BOOLEAN;
			}
			else
			{
				throw new InvalidNumberException("Invalid datatype detected");
			}
		}
		else
		{
			isNumber = false;
		}
	}
	/**
	* Begin to execute operation according to operands and whatever operators are given
	* @param rightOperand: a ResultValue that is to the right of the operation param
	* @param operation: a String that begins the execution of a certain operation with
						the rightOperand and/or and another value
	* @return ResultValue
	*/
	public ResultValue executeOperation(ResultValue rightOperand, String operation) throws Exception
	{
		Object result = 0;
		SubClassif resultType = iDatatype;
		
		if (isNumber)
		{
			if (rightOperand == null)
			{
				// If it's a unary minus
				if (operation.equals("-"))
				{
					result = ((Numeric) value).unaryMinus().toString();
				}
				else
				{
					throw new InvalidOperationException("Invalid operation detected");
				}
			}
			else if (rightOperand.isNumber)
			{
				if (operation.equals("+")) // If it's addition
				{
					result = ((Numeric) value).add((Numeric) rightOperand.value).toString();
				}
				else if (operation.equals("-")) // If it's subtraction
				{
					result = ((Numeric) value).subtract((Numeric) rightOperand.value).toString();
				}
				else if (operation.equals("*")) // If it's multiplication
				{
					result = ((Numeric) value).multiply((Numeric) rightOperand.value).toString();
				}
				else if (operation.equals("/")) // If it's division
				{
					result = ((Numeric) value).divide((Numeric) rightOperand.value).toString();
				}
				else if (operation.equals("^")) // If it's a power
				{
					result = ((Numeric) value).power((Numeric) rightOperand.value).toString();
				}
				else if (operation.equals("==")) // If it's equals
				{
					if (((Numeric) value).equals((Numeric) rightOperand.value) == true)
					{
						result = true;
					}
					else
					{
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
				}
				else if (operation.equals("!=")) // If it's notequals
				{
					if (((Numeric) value).notequals((Numeric) rightOperand.value) == true)
					{
						result = true;
					}
					else
					{
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
				}
				else if (operation.equals("<")) // If it's lessthan
				{
					if (((Numeric) value).lessthan((Numeric) rightOperand.value) == true)
					{
						result = true;
					}
					else
					{
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
				}
				else if (operation.equals("<=")) // If it's lessthanequalto
				{
					if (((Numeric) value).lessthanequalto((Numeric) rightOperand.value) == true)
					{
						result = true;
					}
					else
					{
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
				}
				else if (operation.equals(">")) // If it's greaterthan
				{
					if (((Numeric) value).greaterthan((Numeric) rightOperand.value) == true)
					{
						result = true;
					}
					else
					{
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
				}
				else if (operation.equals(">=")) // If it's greaterthanequalto
				{
					if (((Numeric) value).greaterthanequalto((Numeric) rightOperand.value) == true)
					{
						result = true;
					}
					else
					{
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
				}
				else
				{
					throw new InvalidOperationException("Invalid operation detected");
				}
			}
			else if (rightOperand.iDatatype == SubClassif.STRING)
			{
				Numeric tempRightOperand = 
					new Numeric(((StringBuilder) rightOperand.value).toString(), rightOperand.iDatatype);
				
				if (operation.equals("+"))
				{
					result = ((Numeric) value).add(tempRightOperand).toString();
				}
				else if (operation.equals("-"))
				{
					result = ((Numeric) value).subtract(tempRightOperand).toString();
				}
				else if (operation.equals("*"))
				{
					result = ((Numeric) value).multiply(tempRightOperand).toString();
				}
				else if (operation.equals("/"))
				{
					result = ((Numeric) value).divide(tempRightOperand).toString();
				}
				else if (operation.equals("^"))
				{
					result = ((Numeric) value).power(tempRightOperand).toString();
				}
				else if (operation.equals("=="))
				{
					if (((Numeric) value).equals(tempRightOperand) == true)
					{
						result = true;
					}
					else
					{
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
				}
				else if (operation.equals("!="))
				{
					if (((Numeric) value).notequals(tempRightOperand) == true)
					{
						result = true;
					}
					else
					{
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
				}
				else if (operation.equals("<"))
				{
					if (((Numeric) value).lessthan(tempRightOperand) == true)
					{
						result = true;
					}
					else
					{
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
				}
				else if (operation.equals("<="))
				{
					if (((Numeric) value).lessthanequalto(tempRightOperand) == true)
					{
						result = true;
					}
					else
					{
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
				}
				else if (operation.equals(">"))
				{
					if (((Numeric) value).greaterthan(tempRightOperand) == true)
					{
						result = true;
					}
					else
					{
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
				}
				else if (operation.equals(">="))
				{
					if (((Numeric) value).greaterthanequalto(tempRightOperand) == true)
					{
						result = true;
					}
					else
					{
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
				}
				else
				{
					throw new InvalidOperationException("Invalid operation detected");
				}
			}
			else
			{
				throw new InvalidOperationException("The operand's datatype is invalid for "
					+ operation + " operations");
			}
		}
		else if (iDatatype == SubClassif.BOOLEAN)
		{
			if (rightOperand == null)
			{
				if (operation.equals("not"))
				{
					if (value.equals(true))
					{
						result = false;
					}
					else if (value.equals(false))
					{
						result = true;
					}
					else
					{
						throw new InvalidOperationException("The operand's datatype is invalid for "
							+ operation + " operations");
					}
				}
				else
				{
					throw new InvalidOperationException("Invalid operation detected");
				}
			}
			else if (rightOperand.iDatatype == SubClassif.BOOLEAN)
			{
				// Begin a boolean operation with the rightOperand and the operation passed in
				
				if (operation.equals("and"))
				{
					if ((Boolean) value && (Boolean) rightOperand.value)
					{
						result = true;
					}
					else
					{
						result = false;
					}
				}
				else if (operation.equals("or"))
				{
					if ((Boolean) value || (Boolean) rightOperand.value)
					{
						result = true;
					}
					else
					{
						result = false;
					}
				}
				else if (operation.equals("=="))
				{
					if (value.equals(rightOperand.value))
					{
						result = true;
					}
					else
					{
						result = false;
					}
				}
				else if (operation.equals("!="))
				{
					if (!value.equals(rightOperand.value))
					{
						result = true;
					}
					else
					{
						result = false;
					}
				}
				else
				{
					throw new InvalidOperationException("The operand's datatype is invalid for "
						+ operation + " operations");
				}
			}
			else
			{
				throw new InvalidOperationException("The operand's datatype is invalid for "
					+ operation + " operations");
			}
		}
		else if (iDatatype == SubClassif.STRING)
		{
			String scRightOperand = "";
			
			if (rightOperand.iDatatype == SubClassif.STRING)
			{
				scRightOperand = ((StringBuilder) rightOperand.value).toString();
			}
			else if (rightOperand.iDatatype == SubClassif.BOOLEAN)
			{
				scRightOperand = ((Boolean) rightOperand.value).toString();
			}
			else if (rightOperand.isNumber)
			{
				scRightOperand = ((Numeric) rightOperand.value).toString();
			}
			else
			{
				throw new InvalidOperationException("The operand's datatype is invalid for "
					+ operation + " operations");
			}
			
			if (operation.equals("#"))
			{
				result = new StringBuilder(((StringBuilder) value).toString().concat(scRightOperand));
			}
			else if (operation.equals("+"))
			{
				Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
				Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);
				
				result = leftOp.add(rightOp);
				resultType = leftOp.type;
			}
			else if (operation.equals("-"))
			{
				Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
				Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);
				
				result = leftOp.subtract(rightOp);
				resultType = leftOp.type;
			}
			else if (operation.equals("*"))
			{
				Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
				Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);
				
				result = leftOp.multiply(rightOp);
				resultType = leftOp.type;
			}
			else if (operation.equals("/"))
			{
				Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
				Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);
				
				result = leftOp.divide(rightOp);
				resultType = leftOp.type;
			}
			else if (operation.equals("^"))
			{
				Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
				Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);
				
				result = leftOp.power(rightOp);
				resultType = leftOp.type;
			}
			else if (operation.equals("=="))
			{
				if (((StringBuilder) value).toString().equals(scRightOperand))
				{
					result = true;
				}
				else
				{
					result = false;
				}
				resultType = SubClassif.BOOLEAN;
			}
			else if (operation.equals("!="))
			{
				if (((StringBuilder) value).toString().notequals(scRightOperand))
				{
					result = true;
				}
				else
				{
					result = false;
				}
				resultType = SubClassif.BOOLEAN;
			}
			else if (operation.equals("<"))
			{
				if (((StringBuilder) value).toString().compareTo(scRightOperand) < 0)
				{
					result = true;
				}
				else
				{
					result = false;
				}
				resultType = SubClassif.BOOLEAN;
			}
			else if (operation.equals("<="))
			{
				if (((StringBuilder) value).toString().compareTo(scRightOperand) <= 0)
				{
					result = true;
				}
				else
				{
					result = false;
				}
				resultType = SubClassif.BOOLEAN;
			}
			else if (operation.equals(">"))
			{
				if (((StringBuilder) value).toString().compareTo(scRightOperand) > 0)
				{
					result = true;
				}
				else
				{
					result = false;
				}
				resultType = SubClassif.BOOLEAN;
			}
			else if (operation.equals(">="))
			{
				if (((StringBuilder) value).toString().compareTo(scRightOperand) >= 0)
				{
					result = true;
				}
				else
				{
					result = false;
				}
				resultType = SubClassif.BOOLEAN;
			}
			else
			{
				throw new InvalidOperationException("Invalid operation detected");
			}
		}
		else
		{
			throw new InvalidOperationException("The operand's datatype is invalid for "
				+ operation + " operations");
		}
		return new ResultValue(resultType, result);
	}
	
	@Override
	public String toString()
	{
		return iDatatype + " " + value.toString();
	}
}