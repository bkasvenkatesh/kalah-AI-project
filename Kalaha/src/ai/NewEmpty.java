int minMax(boolean player, int board, int depth )
{ if(player){startambo=Start_S; finishambo=End_S;}
else{startambo=start_N; finishambo=End_N;}
    if(depth==0 || terminal)
    {value= utility(board); return value}
    else
     { for(i=startambo;i<=finishambo,i++)
        { 
            result(i);
            value=minMax(board,player,depth-1);
        }
}
}
    


}