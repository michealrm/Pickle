package pickle;

import pickle.exception.InvalidNumberException;
import pickle.exception.InvalidOperationException;

/**
 * A wrapper for the results of expressions
 *
 * `value` conversions from Pickle type -> Java class
 * Integer	-> Numeric
 * Float	-> Numeric
 * String	-> String
 * Bool		-> Boolean
 *
 * @see pickle.SubClassif for the subclassifications (iDatatype) of the tokens
 */
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
			if ( !(val instanceof Numeric) )
			{
				value = new Numeric( val.toString(), iDatatype);
			}
			isNumber = true;
		}
		
		// Check if type is an identifier type
		else if (iDatatype == SubClassif.IDENTIFIER)
		{
			if (val instanceof Numeric)
			{
				iDatatype = ((Numeric) val).type;
			}
			else if (val instanceof String)
			{
				iDatatype = SubClassif.STRING;
			}
			else if (val instanceof Boolean)
			{
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
		Object result;
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
				switch (operation) {
					case "+":
						result = ((Numeric) value).add((Numeric) rightOperand.value).toString();
						break;
					case "-":
						result = ((Numeric) value).subtract((Numeric) rightOperand.value).toString();
						break;
					case "*":
						result = ((Numeric) value).multiply((Numeric) rightOperand.value).toString();
						break;
					case "/":
						result = ((Numeric) value).divide((Numeric) rightOperand.value).toString();
						break;
					case "^":
						result = ((Numeric) value).power((Numeric) rightOperand.value).toString();
						break;
					case "==":
						if (((Numeric) value).equals((Numeric) rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case "!=":
						if (((Numeric) value).notEqual((Numeric) rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case "<":
						if (((Numeric) value).lessThan((Numeric) rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case "<=":
						if (((Numeric) value).lessThanEqualTo((Numeric) rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case ">":
						if (((Numeric) value).greaterThan((Numeric) rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case ">=":
						if (((Numeric) value).greaterThanEqualTo((Numeric) rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					default:
						throw new InvalidOperationException("Invalid operation detected");
				}
			}
			else if (rightOperand.iDatatype == SubClassif.STRING)
			{
				Numeric tempRightOperand = 
					new Numeric(((StringBuilder) rightOperand.value).toString(), rightOperand.iDatatype);

				switch (operation) {
					case "+":
						result = ((Numeric) value).add(tempRightOperand).toString();
						break;
					case "-":
						result = ((Numeric) value).subtract(tempRightOperand).toString();
						break;
					case "*":
						result = ((Numeric) value).multiply(tempRightOperand).toString();
						break;
					case "/":
						result = ((Numeric) value).divide(tempRightOperand).toString();
						break;
					case "^":
						result = ((Numeric) value).power(tempRightOperand).toString();
						break;
					case "==":
						if (((Numeric) value).equals(tempRightOperand)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case "!=":
						if (((Numeric) value).notEqual(tempRightOperand)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case "<":
						if (((Numeric) value).lessThan(tempRightOperand)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case "<=":
						if (((Numeric) value).lessThanEqualTo(tempRightOperand)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case ">":
						if (((Numeric) value).greaterThan(tempRightOperand)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case ">=":
						if (((Numeric) value).greaterThanEqualTo(tempRightOperand)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					default:
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

				switch (operation) {
					case "and":
						if ((Boolean) value && (Boolean) rightOperand.value) {
							result = true;
						} else {
							result = false;
						}
						break;
					case "or":
						if ((Boolean) value || (Boolean) rightOperand.value) {
							result = true;
						} else {
							result = false;
						}
						break;
					case "==":
						if (value.equals(rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						break;
					case "!=":
						if (!value.equals(rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						break;
					default:
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

			switch (operation) {
				case "#":
					result = new StringBuilder(((StringBuilder) value).toString().concat(scRightOperand));
					break;
				case "+": {
					Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
					Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);

					result = leftOp.add(rightOp);
					resultType = leftOp.type;
					break;
				}
				case "-": {
					Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
					Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);

					result = leftOp.subtract(rightOp);
					resultType = leftOp.type;
					break;
				}
				case "*": {
					Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
					Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);

					result = leftOp.multiply(rightOp);
					resultType = leftOp.type;
					break;
				}
				case "/": {
					Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
					Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);

					result = leftOp.divide(rightOp);
					resultType = leftOp.type;
					break;
				}
				case "^": {
					Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
					Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);

					result = leftOp.power(rightOp);
					resultType = leftOp.type;
					break;
				}
				case "==":
					if (((StringBuilder) value).toString().equals(scRightOperand)) {
						result = true;
					} else {
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
					break;
				case "!=":
					if (((StringBuilder) value).toString().compareTo(scRightOperand) != 0) {
						result = true;
					} else {
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
					break;
				case "<":
					if (((StringBuilder) value).toString().compareTo(scRightOperand) < 0) {
						result = true;
					} else {
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
					break;
				case "<=":
					if (((StringBuilder) value).toString().compareTo(scRightOperand) <= 0) {
						result = true;
					} else {
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
					break;
				case ">":
					if (((StringBuilder) value).toString().compareTo(scRightOperand) > 0) {
						result = true;
					} else {
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
					break;
				case ">=":
					if (((StringBuilder) value).toString().compareTo(scRightOperand) >= 0) {
						result = true;
					} else {
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
					break;
				default:
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

	public boolean equals(ResultValue rv) {
		return (iDatatype == rv.iDatatype && value == rv.value);
	}

	@Override
	public String toString()
	{
		return iDatatype + " " + value.toString();
	}
}