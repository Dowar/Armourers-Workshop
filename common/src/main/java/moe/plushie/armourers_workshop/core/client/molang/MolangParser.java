package moe.plushie.armourers_workshop.core.client.molang;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import moe.plushie.armourers_workshop.core.client.molang.expressions.MolangCompoundValue;
import moe.plushie.armourers_workshop.core.client.molang.expressions.MolangValue;
import moe.plushie.armourers_workshop.core.client.molang.expressions.MolangVariableHolder;
import moe.plushie.armourers_workshop.core.client.molang.functions.utility.CosDegrees;
import moe.plushie.armourers_workshop.core.client.molang.functions.utility.SinDegrees;
import moe.plushie.armourers_workshop.core.client.molang.math.Constant;
import moe.plushie.armourers_workshop.core.client.molang.math.IValue;
import moe.plushie.armourers_workshop.core.client.molang.math.LazyVariable;
import moe.plushie.armourers_workshop.core.client.molang.math.MathBuilder;
import moe.plushie.armourers_workshop.core.client.molang.math.Variable;

import java.util.List;

/**
 * Utility class for parsing and utilising MoLang functions and expressions
 *
 * @see <a href="https://bedrock.dev/docs/1.19.0.0/1.19.30.23/Molang#Math%20Functions">Bedrock Dev - Molang</a>
 */
public class MolangParser extends MathBuilder {

    public static final MolangVariableHolder ZERO = new MolangVariableHolder(null, new Constant(0));

    public static final MolangVariableHolder ONE = new MolangVariableHolder(null, new Constant(1));

    public static final String RETURN = "return ";

    public MolangParser() {
        super();
        // Remap functions to be intact with Molang specification
        doCoreRemaps();
    }

    public MolangValue parseJson(JsonElement element) throws MolangException {
        if (!element.isJsonPrimitive()) {
            return ZERO;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isNumber()) {
            return new MolangValue(new Constant(primitive.getAsDouble()));
        }
        if (primitive.isString()) {
            String string = primitive.getAsString();
            try {
                return new MolangValue(new Constant(Double.parseDouble(string)));
            } catch (NumberFormatException ex) {
                return parseExpression(string);
            }
        }
        return ZERO;
    }

    /**
     * Parse a molang expression
     */
    public MolangValue parseExpression(String expression) throws MolangException {
        MolangCompoundValue result = null;
        for (String split : expression.toLowerCase().trim().split(";")) {
            String trimmed = split.trim();
            if (!trimmed.isEmpty()) {
                if (result == null) {
                    result = new MolangCompoundValue(parseOneLine(trimmed, result));
                    continue;
                }
                result.values.add(parseOneLine(trimmed, result));
            }
        }
        if (result == null) {
            throw new MolangException("Molang expression cannot be blank!");
        }
        return result;
    }

    /**
     * Parse a single Molang statement
     */
    protected MolangValue parseOneLine(String expression, MolangCompoundValue currentStatement) throws MolangException {
        if (expression.startsWith(RETURN)) {
            try {
                return new MolangValue(parse(expression.substring(RETURN.length())), true);
            } catch (Exception e) {
                throw new MolangException("Couldn't parse return '" + expression + "' expression!");
            }
        }

        try {
            List<Object> symbols = breakdownChars(breakdown(expression));

            if (symbols.size() >= 3 && symbols.get(0) instanceof String && isVariable(symbols.get(0)) && symbols.get(1).equals("=")) {
                symbols = symbols.subList(2, symbols.size());
                Variable variable;
                String name = (String) symbols.get(0);
                if (!variables.containsKey(name) && !currentStatement.locals.containsKey(name)) {
                    LazyVariable local = new LazyVariable(name, 0);
                    currentStatement.locals.put(name, local);
                    variable = local;
                } else {
                    variable = getVariable(name, currentStatement);
                }

                return new MolangVariableHolder(variable, parseSymbolsMolang(symbols));
            }

            return new MolangValue(parseSymbolsMolang(symbols));
        } catch (Exception e) {
            throw new MolangException("Couldn't parse '" + expression + "' expression!");
        }
    }

    private void doCoreRemaps() {
        // Replace radian based sin and cos with degree-based functions
        this.functions.put("cos", CosDegrees.class);
        this.functions.put("sin", SinDegrees.class);

        remap("abs", "math.abs");
        remap("acos", "math.acos");
        remap("asin", "math.asin");
        remap("atan", "math.atan");
        remap("atan2", "math.atan2");
        remap("ceil", "math.ceil");
        remap("clamp", "math.clamp");
        remap("cos", "math.cos");
        remap("die_roll", "math.die_roll");
        remap("die_roll_integer", "math.die_roll_integer");
        remap("exp", "math.exp");
        remap("floor", "math.floor");
        remap("hermite_blend", "math.hermite_blend");
        remap("lerp", "math.lerp");
        remap("lerprotate", "math.lerprotate");
        remap("ln", "math.ln");
        remap("max", "math.max");
        remap("min", "math.min");
        remap("mod", "math.mod");
        remap("pi", "math.pi");
        remap("pow", "math.pow");
        remap("random", "math.random");
        remap("random_integer", "math.random_integer");
        remap("round", "math.round");
        remap("sin", "math.sin");
        remap("sqrt", "math.sqrt");
        remap("trunc", "math.trunc");
    }

    /**
     * Remap a function to a new name, maintaining the actual functionality and removing the old registration entry
     */
    public void remap(String old, String newName) {
        this.functions.put(newName, this.functions.remove(old));
    }

    /**
     * Get the registered {@link LazyVariable} for the given name
     *
     * @param name The name of the variable to get
     * @return The registered {@code LazyVariable} instance, or a newly registered instance if one wasn't registered
     * previously
     */
    @Override
    public Variable getVariable(String name) {
        return variables.computeIfAbsent(name, key -> new LazyVariable(key, 0));
    }

    public Variable getVariable(String name, MolangCompoundValue currentStatement) {
        LazyVariable variable;
        if (currentStatement != null) {
            variable = currentStatement.locals.get(name);
            if (variable != null) {
                return variable;
            }
        }
        return getVariable(name);
    }

    /**
     * Wrapper around {@link #parseSymbols(List)} to throw {@link MolangException}
     */
    private IValue parseSymbolsMolang(List<Object> symbols) throws MolangException {
        try {
            return this.parseSymbols(symbols);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MolangException("Couldn't parse an expression!");
        }
    }

    /**
     * Extend this method to allow {@link #breakdownChars(String[])} to capture "=" as an operator, so it was easier to
     * parse assignment statements
     */
    @Override
    protected boolean isOperator(String s) {
        return super.isOperator(s) || s.equals("=");
    }
}
