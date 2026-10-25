import { Budget } from './budget.model';

export interface Cost {
  id?: string;
  amount: number;
  name: string;
  budget: Budget;
}
