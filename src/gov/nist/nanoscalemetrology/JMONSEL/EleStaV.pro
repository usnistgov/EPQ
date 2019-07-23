/* -------------------------------------------------------------------
   File "EleSta_v.pro"

   Electrostatics - Electric scalar potential v formulation
   ------------------------------------------------------------------- 

   I N P U T
   ---------

   Global Groups :  (Extension '_Ele' is for Electric problem)
   -------------
   Domain_Ele               Whole electric domain
   DomainCC_Ele             Nonconducting regions
   DomainC_Ele              Conducting regions

   Function :
   --------
   epsr[]                   Relative permittivity

   Constraint :
   ----------
   ElectricScalarPotential  Fixed electric scalar potential
                            (classical boundary condition)

   Physical constants :
   ------------------                                               */

   eps0 = 8.854187818e-12;
   chargeUnit = 1.60217653e-19; /* mag of electron charge */

Group {
  DefineGroup[ Domain_Ele, DomainCC_Ele, DomainC_Ele ];
}

Function {
  DefineFunction[ epsr ];
  DefineFunction[ q ];
}

FunctionSpace {
  { Name Hgrad_v_Ele; Type Form0;
    BasisFunction {
      // v = v  s   ,  for all nodes
      //      n  n
      { Name sn; NameOfCoef vn; Function BF_Node;
        Support Region[{Domain_Ele,DomainC_Ele}]; Entity NodesOf[ All , Not FloatConstrained ]; }
    }
    Constraint {
      { NameOfCoef vn; EntityType NodesOf; 
        NameOfConstraint ElectricScalarPotential; }
    }
  }
}

Formulation {
  { Name Electrostatics_v; Type FemEquation;
    Quantity {
      { Name v; Type Local; NameOfSpace Hgrad_v_Ele; }
    }
    Equation {
      Galerkin { [ epsr[] * Dof{d v} , {d v} ]; In Domain_Ele; 
                 Jacobian Vol; Integration GradGrad; }
      Galerkin { [ -q[]*chargeUnit/eps0/ElementVol[] , {v} ]; In SourceDomain; 
                 Jacobian Vol; Integration GradGrad; }
    }
  }
}


Resolution {
  { Name EleSta_v;
    System {
      { Name Sys_Ele; NameOfFormulation Electrostatics_v; }
    }
    Operation { 
      Generate[Sys_Ele]; Solve[Sys_Ele]; SaveSolution[Sys_Ele];
    }
  }
}


PostProcessing {
  { Name EleSta_v; NameOfFormulation Electrostatics_v;
    Quantity {
      { Name v; 
        Value { 
          Local { [ {v} ]; In DomainCC_Ele; Jacobian Vol; } 
        }
      }
      { Name e; 
        Value { 
          Local { [ -{d v} ]; In DomainCC_Ele; Jacobian Vol; }
        }
      }
      { Name d; 
        Value { 
          Local { [ -eps0*epsr[] * {d v} ]; In DomainCC_Ele; 
                                             Jacobian Vol; } 
        } 
      }
    }
  }
}
