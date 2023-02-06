import { TabBar } from '../components/TabBar';
import { Temp } from '../components/temp/Temp';

export const TempPage = () => {
  return (
    <main className="base-layout">
      <Temp />
      <TabBar />
    </main>
  );
};
