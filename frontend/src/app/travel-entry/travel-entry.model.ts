import { Budget } from '../models/budget.model';

export interface TravelEntry {
  id?: string;
  title: string;
  destination: string;
  startDate: string;
  endDate: string;
  description?: string;
  budget: Budget;
  userId?: string;
  color?: string;
}
