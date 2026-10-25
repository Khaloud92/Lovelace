import { Budget } from './budget.model';


export interface Cost {
  cost: number;
  id?: string;
  budget: Budget;
  budgetId: string;
  amount: number;
  name: string;
}
