// This file was auto-generated by the veyron vdl tool.
// Source: advanced.vdl
package com.veyron2.vdl.test_arith;

import com.veyron2.vdl.test_arith.exp.Exp;

/**
 * AdvancedMath is an interface for more advanced math than arith.  It embeds
 * interfaces defined both in the same file and in an external package; and in
 * turn it is embedded by arith.Calculator (which is in the same package but
 * different file) to verify that embedding works in all these scenarios.
 */
public interface AdvancedMath extends Trigonometry, Exp { 
}
