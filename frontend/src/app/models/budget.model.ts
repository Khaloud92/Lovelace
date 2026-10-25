import { Cost } from './cost.model';

export interface BudgetTravelEntry {
  id: string;
  startDate: string;
  endDate: string;
  color?: string;
}

export interface Budget {
  id: string;
  name: string;
  amount: number;
  costs?: Cost[];
  travelEntry?: BudgetTravelEntry;
}
