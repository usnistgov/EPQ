/* The formulation used and its tools, considered as being 
   in a black box, can now be included */

Include "Jacobian_Lib.pro"
Include "Integration_Lib.pro"
Include "EleSta_v.pro"

/* Finally, we can define some operations to output results */

e = 1.e-7;

PostOperation {
  { Name Map; NameOfPostProcessing EleSta_v;
     Operation {
       Print [ v, OnElementsOf DomainCC_Ele, File "Lines_v.pos" ];
       //Print [ e, OnElementsOf DomainCC_Ele, File "Lines_e.pos" ];
     }
  }
  /*{ Name Cut; NameOfPostProcessing EleSta_v;
     Operation {
       Print [ e, OnLine {{e,e,0}{10.e-3,e,0}} {500}, File "Cut_e" ];
     }
  }*/

}
